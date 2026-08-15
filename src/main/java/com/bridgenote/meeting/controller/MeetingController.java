package com.bridgenote.meeting.controller;

import com.bridgenote.common.jwt.AuthUser;
import com.bridgenote.common.jwt.CurrentUser;
import com.bridgenote.meeting.dto.MeetingConsentReqDto;
import com.bridgenote.meeting.dto.MeetingConsentResDto;
import com.bridgenote.meeting.dto.MeetingCreateReqDto;
import com.bridgenote.meeting.dto.MeetingCreateResDto;
import com.bridgenote.meeting.dto.MeetingDetailResDto;
import com.bridgenote.meeting.dto.MeetingEndResDto;
import com.bridgenote.meeting.dto.MeetingJoinReqDto;
import com.bridgenote.meeting.dto.MeetingJoinResDto;
import com.bridgenote.meeting.dto.CulturalNoteResDto;
import com.bridgenote.meeting.dto.MeetingListResDto;
import com.bridgenote.meeting.dto.MinutesResultResDto;
import com.bridgenote.meeting.dto.TranscriptResDto;
import com.bridgenote.meeting.service.MeetingArchiveService;
import com.bridgenote.meeting.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "회의", description = "회의 생성/조회/참가/종료 API")
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

	private final MeetingService meetingService;
	private final MeetingArchiveService meetingArchiveService;

	@Operation(summary = "회의 생성", description = "로그인 사용자가 새 회의를 만들고 초대 코드를 발급받습니다.")
	@PostMapping
	public ResponseEntity<MeetingCreateResDto> create(
			@CurrentUser AuthUser user,
			@Valid @RequestBody MeetingCreateReqDto request
	) {
		MeetingCreateResDto response = meetingService.create(user, request);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@Operation(summary = "내 회의 목록 조회", description = "로그인 사용자가 주최했거나 참가한 회의 목록을 조회합니다.")
	@GetMapping
	public ResponseEntity<List<MeetingListResDto>> myMeetings(@CurrentUser AuthUser user) {
		return ResponseEntity.ok(meetingService.getMyMeetings(user));
	}

	@Operation(summary = "회의 정보 / 참가자 조회", description = "특정 회의의 정보와 참가자 목록(언어·화자 번호 포함)을 조회합니다.")
	@GetMapping("/{id}")
	public ResponseEntity<MeetingDetailResDto> getMeeting(
			@CurrentUser AuthUser user,
			@PathVariable String id
	) {
		return ResponseEntity.ok(meetingService.getMeeting(id));
	}

	@Operation(summary = "데이터 처리 동의", description = "회의 입장 전 음성/텍스트 데이터 처리에 동의합니다. 미동의 시 join이 차단됩니다.")
	@PostMapping("/{id}/consent")
	public ResponseEntity<MeetingConsentResDto> consent(
			@CurrentUser AuthUser user,
			@PathVariable String id,
			@Valid @RequestBody MeetingConsentReqDto request
	) {
		return ResponseEntity.ok(meetingService.consent(user, id, request));
	}

	@Operation(summary = "회의 참가", description = "초대받은 사용자가 회의에 참가하고 화자 번호를 배정받습니다.")
	@PostMapping("/{id}/join")
	public ResponseEntity<MeetingJoinResDto> join(
			@CurrentUser AuthUser user,
			@PathVariable String id,
			@RequestBody MeetingJoinReqDto request
	) {
		return ResponseEntity.ok(meetingService.join(user, id, request));
	}

	@Operation(summary = "회의 종료", description = "주최자가 회의를 종료하고 회의록 생성 파이프라인을 트리거합니다.")
	@PostMapping("/{id}/end")
	public ResponseEntity<MeetingEndResDto> end(
			@CurrentUser AuthUser user,
			@PathVariable String id
	) {
		return ResponseEntity.ok(meetingService.endMeeting(user, id));
	}

	@Operation(summary = "회의록 조회",
			description = "생성된 회의록을 조회합니다. 생성 중이면 status=pending, 완료 시 status=ready + 언어·직무별 섹션.")
	@GetMapping("/{id}/minutes")
	public ResponseEntity<MinutesResultResDto> minutes(
			@CurrentUser AuthUser user,
			@PathVariable String id
	) {
		return ResponseEntity.ok(meetingService.getMinutes(id));
	}

	@Operation(summary = "전체 전사 기록 조회",
			description = "회의의 확정 발화를 시간순으로 조회합니다(원문+언어별 번역, 페이지네이션).")
	@GetMapping("/{id}/utterances")
	public ResponseEntity<TranscriptResDto> utterances(
			@CurrentUser AuthUser user,
			@PathVariable String id,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size
	) {
		return ResponseEntity.ok(meetingArchiveService.getTranscript(id, PageRequest.of(page, size)));
	}

	@Operation(summary = "문화 가이드(각주) 조회",
			description = "회의의 문화 각주 목록과 note_type별 집계를 조회합니다.")
	@GetMapping("/{id}/cultural-notes")
	public ResponseEntity<CulturalNoteResDto> culturalNotes(
			@CurrentUser AuthUser user,
			@PathVariable String id
	) {
		return ResponseEntity.ok(meetingArchiveService.getCulturalNotes(id));
	}
}
