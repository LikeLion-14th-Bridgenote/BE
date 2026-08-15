package com.bridgenote.meeting.service;

import com.bridgenote.ai.client.AiMinutesClient;
import com.bridgenote.ai.dto.MinutesRequest;
import com.bridgenote.ai.dto.MinutesResponse;
import com.bridgenote.meeting.domain.MeetingMinutes;
import com.bridgenote.meeting.dto.MinutesResultResDto;
import com.bridgenote.meeting.repository.MeetingMinutesRepository;
import com.bridgenote.participant.domain.Participant;
import com.bridgenote.participant.repository.ParticipantRepository;
import com.bridgenote.realtime.domain.Utterance;
import com.bridgenote.realtime.repository.UtteranceRepository;
import com.bridgenote.user.domain.User;
import com.bridgenote.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 회의록 생성 오케스트레이션 (회의 종료 → AI /ai/minutes).
 * <ol>
 *   <li>{@link #trigger}: 상태 PENDING 기록 후, 확정 발화·참가자(언어/직무)를 모아 AI 비동기 호출</li>
 *   <li>응답 도착 시 minutes JSON을 저장(READY), 실패 시 FAILED</li>
 *   <li>{@link #read}: 저장된 회의록을 상태와 함께 반환(pending/ready/failed)</li>
 * </ol>
 * 배치가 비동기라 회의 종료 응답을 막지 않는다. FE는 GET으로 상태를 폴링한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinutesGenerator {

	private final UtteranceRepository utteranceRepository;
	private final ParticipantRepository participantRepository;
	private final UserRepository userRepository;
	private final MeetingMinutesRepository meetingMinutesRepository;
	private final AiMinutesClient aiMinutesClient;
	private final ObjectMapper objectMapper;

	/** 회의 종료 시 호출. 회의록 생성을 비동기로 트리거한다. */
	@Transactional
	public void trigger(String meetingId) {
		MeetingMinutes row = meetingMinutesRepository.findById(meetingId)
				.orElseGet(() -> new MeetingMinutes(meetingId));
		row.markPending();
		meetingMinutesRepository.save(row);

		List<Utterance> utterances = utteranceRepository.findByMeetingIdOrderBySpokenAtAsc(meetingId);
		if (utterances.isEmpty()) {
			storeReady(meetingId, List.of()); // 발화 없음 → 빈 회의록으로 즉시 완료(AI 호출 불필요)
			return;
		}

		List<Participant> participants = participantRepository
				.findByMeetingIdAndSpeakerIndexIsNotNullOrderBySpeakerIndexAsc(meetingId);

		Map<Integer, String> speakerNames = new HashMap<>();
		Set<String> languages = new LinkedHashSet<>();
		Set<String> jobRoles = new LinkedHashSet<>();
		for (Participant p : participants) {
			if (p.getSpeakerIndex() != null && p.getNickname() != null) {
				speakerNames.put(p.getSpeakerIndex(), p.getNickname());
			}
			if (p.getLanguage() != null) {
				languages.add(p.getLanguage());
			}
			userRepository.findByAuthUserId(p.getProfileId())
					.map(User::getJob).filter(Objects::nonNull).ifPresent(jobRoles::add);
		}

		List<MinutesRequest.Utterance> items = utterances.stream()
				.map(u -> new MinutesRequest.Utterance(
						u.getSentenceId(),
						speakerNames.get(u.getSpeakerIndex()),
						u.getSourceLang(),
						u.getSourceText()))
				.toList();

		MinutesRequest request = new MinutesRequest(
				meetingId, new ArrayList<>(languages), new ArrayList<>(jobRoles), items);

		aiMinutesClient.generate(request,
				resp -> storeReady(meetingId, resp.minutes() != null ? resp.minutes() : List.of()),
				err -> storeFailed(meetingId, err));
	}

	/** 저장된 회의록 조회. 없거나 대기 중이면 pending. */
	@Transactional(readOnly = true)
	public MinutesResultResDto read(String meetingId) {
		MeetingMinutes row = meetingMinutesRepository.findById(meetingId).orElse(null);
		if (row == null || row.getStatus() == MeetingMinutes.Status.PENDING) {
			return MinutesResultResDto.of(meetingId, "pending", List.of());
		}
		if (row.getStatus() == MeetingMinutes.Status.FAILED) {
			return MinutesResultResDto.of(meetingId, "failed", List.of());
		}
		List<MinutesResponse.Section> sections = List.of();
		if (row.getMinutesJson() != null) {
			try {
				sections = List.of(objectMapper.readValue(
						row.getMinutesJson(), MinutesResponse.Section[].class));
			} catch (RuntimeException e) {
				log.warn("회의록 역직렬화 실패 meeting={}", meetingId, e);
				return MinutesResultResDto.of(meetingId, "failed", List.of());
			}
		}
		return MinutesResultResDto.of(meetingId, "ready", sections);
	}

	/** AI 응답의 minutes 섹션을 JSON으로 저장(READY). 비동기 콜백에서 호출. */
	private void storeReady(String meetingId, List<MinutesResponse.Section> minutes) {
		final String json;
		try {
			json = objectMapper.writeValueAsString(minutes); // Jackson 3: unchecked
		} catch (RuntimeException e) {
			storeFailed(meetingId, e);
			return;
		}
		MeetingMinutes row = meetingMinutesRepository.findById(meetingId)
				.orElseGet(() -> new MeetingMinutes(meetingId));
		row.markReady(json);
		meetingMinutesRepository.save(row);
		log.info("회의록 생성 완료 meeting={} sections={}", meetingId, minutes.size());
	}

	private void storeFailed(String meetingId, Throwable err) {
		log.warn("회의록 생성 실패 meeting={}: {}", meetingId, err.toString());
		MeetingMinutes row = meetingMinutesRepository.findById(meetingId)
				.orElseGet(() -> new MeetingMinutes(meetingId));
		row.markFailed();
		meetingMinutesRepository.save(row);
	}
}
