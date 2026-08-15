package com.bridgenote.meeting.service;

import com.bridgenote.common.exception.BusinessException;
import com.bridgenote.common.jwt.AuthUser;
import com.bridgenote.meeting.domain.Meeting;
import com.bridgenote.meeting.domain.MeetingStatus;
import com.bridgenote.meeting.dto.MeetingConsentReqDto;
import com.bridgenote.meeting.dto.MeetingConsentResDto;
import com.bridgenote.meeting.dto.MeetingCreateReqDto;
import com.bridgenote.meeting.dto.MeetingCreateResDto;
import com.bridgenote.meeting.dto.MeetingDetailResDto;
import com.bridgenote.meeting.dto.MeetingEndResDto;
import com.bridgenote.meeting.dto.MeetingJoinReqDto;
import com.bridgenote.meeting.dto.MeetingJoinResDto;
import com.bridgenote.meeting.dto.MeetingListResDto;
import com.bridgenote.meeting.repository.MeetingRepository;
import com.bridgenote.participant.domain.Participant;
import com.bridgenote.participant.dto.ParticipantResDto;
import com.bridgenote.participant.repository.ParticipantRepository;
import com.bridgenote.realtime.dto.MeetingEndedMessage;
import com.bridgenote.realtime.session.WebSocketSessionRegistry;
import com.bridgenote.user.domain.User;
import com.bridgenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MeetingService {

	private static final String CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
	private static final int CODE_LENGTH = 6;
	private static final int MAX_RETRY = 10;
	private static final SecureRandom RANDOM = new SecureRandom();

	private final MeetingRepository meetingRepository;
	private final ParticipantRepository participantRepository;
	// 임시 email 브릿지용(JWT email ↔ 수민 User). id 정렬 후 profileId 조회로 전환 예정.
	private final UserRepository userRepository;
	// 회의 종료 시 참가자 전원에게 meeting_ended 브로드캐스트
	private final WebSocketSessionRegistry sessionRegistry;

	@Value("${bridgenote.invite-base-url}")
	private String inviteBaseUrl;

	/**
	 * 회의를 생성한다. 초대 코드는 중복되지 않게 발급하고, host는 로그인 사용자.
	 */
	@Transactional
	public MeetingCreateResDto create(AuthUser host, MeetingCreateReqDto req) {
		String inviteCode = generateUniqueInviteCode();

		Meeting meeting = Meeting.builder()
				.hostId(host.id())
				.title(req.title())
				.description(req.description())
				.expectedCount(req.expectedCount())
				.inviteCode(inviteCode)
				.build();

		Meeting saved = meetingRepository.save(meeting);
		// 초대 링크 하나로 참가 가능하도록 code를 쿼리로 포함 (프론트가 id+code 파싱 → join 호출)
		String inviteUrl = inviteBaseUrl + "/meetings/" + saved.getId() + "?code=" + saved.getInviteCode();
		return MeetingCreateResDto.from(saved, inviteUrl);
	}

	/**
	 * 로그인 사용자가 주최했거나 참가한(입장 완료) 회의 목록. 각 회의의 입장 인원 포함.
	 */
	@Transactional(readOnly = true)
	public List<MeetingListResDto> getMyMeetings(AuthUser user) {
		List<Meeting> meetings = meetingRepository.findMyMeetings(user.id());
		if (meetings.isEmpty()) {
			return List.of();
		}

		List<String> meetingIds = meetings.stream().map(Meeting::getId).toList();
		Map<String, Long> countByMeeting = new HashMap<>();
		for (Object[] row : participantRepository.countJoinedGroupedByMeeting(meetingIds)) {
			countByMeeting.put((String) row[0], (Long) row[1]);
		}

		return meetings.stream()
				.map(m -> MeetingListResDto.of(m, countByMeeting.getOrDefault(m.getId(), 0L)))
				.toList();
	}

	/**
	 * 회의 상세(정보 + 입장한 참가자 목록). 없으면 404.
	 */
	@Transactional(readOnly = true)
	public MeetingDetailResDto getMeeting(String meetingId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "해당 회의를 찾을 수 없습니다."));

		List<ParticipantResDto> participants = participantRepository
				.findByMeetingIdAndSpeakerIndexIsNotNullOrderBySpeakerIndexAsc(meetingId).stream()
				.map(ParticipantResDto::from)
				.toList();

		return MeetingDetailResDto.of(meeting, participants);
	}

	/**
	 * 데이터 처리 동의. 참가자 행을 만들거나 갱신해 동의 여부를 기록한다. (join 전 필수)
	 */
	@Transactional
	public MeetingConsentResDto consent(AuthUser user, String meetingId, MeetingConsentReqDto req) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "해당 회의를 찾을 수 없습니다."));

		Participant participant = participantRepository
				.findByMeetingIdAndProfileId(meetingId, user.id())
				.orElseGet(() -> participantRepository.save(Participant.builder()
						.meetingId(meetingId)
						.profileId(user.id())
						.build()));

		participant.applyConsent(req.agreed(), Instant.now());
		return new MeetingConsentResDto(meetingId, participant.isConsented(), participant.getConsentedAt());
	}

	/**
	 * 회의 참가. 초대 코드 검증 + 동의 확인 후 화자 번호 배정. 미동의/미동의행이면 차단.
	 * profile은 email 브릿지로 조회(임시). 없으면 null 필드.
	 */
	@Transactional
	public MeetingJoinResDto join(AuthUser user, String meetingId, MeetingJoinReqDto req) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "해당 회의를 찾을 수 없습니다."));

		String inviteCode = (req == null) ? null : req.inviteCode();
		if (inviteCode == null || inviteCode.isBlank() || !inviteCode.equals(meeting.getInviteCode())) {
			throw new BusinessException(HttpStatus.BAD_REQUEST,
					"잘못된 요청입니다. 초대 코드가 누락되었거나 형식이 올바르지 않습니다.");
		}

		Participant participant = participantRepository
				.findByMeetingIdAndProfileId(meetingId, user.id())
				.filter(Participant::isConsented)
				.orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST,
						"데이터 처리 동의가 필요합니다. 회의 참가 전 동의해주세요."));

		// profile (임시 email 브릿지) — id 정렬되면 findById(profileId)로 전환
		User profile = userRepository.findByEmail(user.email()).orElse(null);
		String nickname = (profile != null) ? profile.getName() : null;
		String language = (profile != null) ? profile.getLanguage() : null;
		String culture = (profile != null) ? profile.getCulture() : null;
		String jobRole = (profile != null) ? profile.getJob() : null;

		if (!participant.isJoined()) {
			int speakerIndex = (int) participantRepository.countByMeetingIdAndSpeakerIndexIsNotNull(meetingId);
			participant.completeJoin(nickname, language, speakerIndex, Instant.now());
		}

		return new MeetingJoinResDto(
				participant.getId(),
				meetingId,
				participant.getSpeakerIndex(),
				new MeetingJoinResDto.ProfileDto(nickname, language, culture, jobRole));
	}

	/**
	 * 회의 종료(주최자만). 상태 ENDED + 종료시각 기록 후 회의록 파이프라인 트리거.
	 */
	@Transactional
	public MeetingEndResDto endMeeting(AuthUser user, String meetingId) {
		Meeting meeting = meetingRepository.findById(meetingId)
				.orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "해당 회의를 찾을 수 없습니다."));

		if (!meeting.getHostId().equals(user.id())) {
			throw new BusinessException(HttpStatus.FORBIDDEN,
					"회의를 종료할 권한이 없습니다. 주최자만 종료할 수 있습니다.");
		}

		meeting.end(Instant.now());
		// 연결 중인 참가자 전원에게 종료 알림 (프론트가 즉시 회의록 대기 화면으로 전환)
		sessionRegistry.broadcast(meetingId, MeetingEndedMessage.of(meetingId, meeting.getEndedAt()));
		// TODO: 회의록 생성 파이프라인 트리거 (AI 서버 /ai/minutes 배치 호출) — realtime/AI 연동 시 구현
		return MeetingEndResDto.from(meeting);
	}

	/**
	 * 참가자 WS 접속 시 회의 상태 처리(핸들러가 호출).
	 * 없으면 NOT_FOUND, ENDED면 접속 거부용 ENDED, WAITING이면 첫 접속에서 LIVE로 전환하고 STARTED, 이미 진행 중이면 LIVE.
	 */
	@Transactional
	public ConnectResult onParticipantConnect(String meetingId) {
		Meeting meeting = meetingRepository.findById(meetingId).orElse(null);
		if (meeting == null) {
			return ConnectResult.NOT_FOUND;
		}
		if (meeting.getStatus() == MeetingStatus.ENDED) {
			return ConnectResult.ENDED;
		}
		return meeting.start(Instant.now()) ? ConnectResult.STARTED : ConnectResult.LIVE;
	}

	/** WS 접속 시 회의 상태 판정 결과. */
	public enum ConnectResult { STARTED, LIVE, ENDED, NOT_FOUND }

	private String generateUniqueInviteCode() {
		for (int i = 0; i < MAX_RETRY; i++) {
			String code = randomCode();
			if (!meetingRepository.existsByInviteCode(code)) {
				return code;
			}
		}
		throw new IllegalStateException("초대 코드 생성에 실패했습니다. 다시 시도해주세요.");
	}

	private String randomCode() {
		StringBuilder sb = new StringBuilder(CODE_LENGTH);
		for (int i = 0; i < CODE_LENGTH; i++) {
			sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
		}
		return sb.toString();
	}
}
