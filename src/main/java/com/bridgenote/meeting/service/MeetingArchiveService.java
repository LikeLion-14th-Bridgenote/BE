package com.bridgenote.meeting.service;

import com.bridgenote.common.exception.BusinessException;
import com.bridgenote.meeting.dto.CulturalNoteResDto;
import com.bridgenote.meeting.dto.TranscriptResDto;
import com.bridgenote.meeting.repository.MeetingRepository;
import com.bridgenote.participant.domain.Participant;
import com.bridgenote.participant.repository.ParticipantRepository;
import com.bridgenote.realtime.domain.CulturalNote;
import com.bridgenote.realtime.domain.Utterance;
import com.bridgenote.realtime.domain.UtteranceTranslation;
import com.bridgenote.realtime.repository.CulturalNoteRepository;
import com.bridgenote.realtime.repository.UtteranceRepository;
import com.bridgenote.realtime.repository.UtteranceTranslationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 종료된 회의의 아카이브 조회 — 전체 전사 기록(원문+번역), 문화 가이드(각주 집계).
 * 실시간 파이프라인이 저장해둔 Utterance/UtteranceTranslation/CulturalNote를 읽어 회의록 페이지에 제공한다.
 */
@Service
@RequiredArgsConstructor
public class MeetingArchiveService {

	private final MeetingRepository meetingRepository;
	private final ParticipantRepository participantRepository;
	private final UtteranceRepository utteranceRepository;
	private final UtteranceTranslationRepository utteranceTranslationRepository;
	private final CulturalNoteRepository culturalNoteRepository;

	/** 전체 전사 기록(페이지네이션). 시간순 발화 + 언어별 번역. */
	@Transactional(readOnly = true)
	public TranscriptResDto getTranscript(String meetingId, Pageable pageable) {
		assertMeetingExists(meetingId);

		Page<Utterance> page = utteranceRepository.findByMeetingIdOrderBySpokenAtAsc(meetingId, pageable);
		Map<Integer, String> speakerNames = speakerNames(meetingId);

		List<String> sentenceIds = page.getContent().stream().map(Utterance::getSentenceId).toList();
		Map<String, List<TranscriptResDto.Translation>> transBySentence = sentenceIds.isEmpty()
				? Map.of()
				: utteranceTranslationRepository.findBySentenceIdIn(sentenceIds).stream()
						.collect(Collectors.groupingBy(UtteranceTranslation::getSentenceId,
								Collectors.mapping(t -> new TranscriptResDto.Translation(t.getLang(), t.getText()),
										Collectors.toList())));

		List<TranscriptResDto.Row> rows = page.getContent().stream()
				.map(u -> new TranscriptResDto.Row(
						u.getSentenceId(),
						u.getSpeakerIndex(),
						speakerNames.get(u.getSpeakerIndex()),
						u.getSpokenAt(),
						u.getSourceLang(),
						u.getSourceText(),
						transBySentence.getOrDefault(u.getSentenceId(), List.of())))
				.toList();

		return new TranscriptResDto(meetingId, page.getNumber(), page.getSize(),
				page.getTotalPages(), page.getTotalElements(), rows);
	}

	/** 문화 가이드 — 각주 목록 + note_type별 집계. */
	@Transactional(readOnly = true)
	public CulturalNoteResDto getCulturalNotes(String meetingId) {
		assertMeetingExists(meetingId);

		List<CulturalNote> notes = culturalNoteRepository.findByMeetingIdOrderByCreatedAtAsc(meetingId);
		Map<Integer, String> speakerNames = speakerNames(meetingId);

		List<CulturalNoteResDto.TypeCount> byType = notes.stream()
				.collect(Collectors.groupingBy(
						n -> n.getNoteType() != null ? n.getNoteType() : "기타",
						Collectors.counting()))
				.entrySet().stream()
				.map(e -> new CulturalNoteResDto.TypeCount(e.getKey(), e.getValue()))
				.toList();

		List<CulturalNoteResDto.Note> noteDtos = notes.stream()
				.map(n -> new CulturalNoteResDto.Note(
						n.getSentenceId(),
						n.getSpeakerIndex(),
						speakerNames.get(n.getSpeakerIndex()),
						n.getRiskLevel(),
						n.getNoteType(),
						n.getSpeakerIntent(),
						n.getListenerMisread(),
						n.getAdvice(),
						n.getRewriteText(),
						n.getCreatedAt()))
				.toList();

		return new CulturalNoteResDto(meetingId, notes.size(), byType, noteDtos);
	}

	/** 화자 번호 → 닉네임 매핑(표시용). */
	private Map<Integer, String> speakerNames(String meetingId) {
		return participantRepository
				.findByMeetingIdAndSpeakerIndexIsNotNullOrderBySpeakerIndexAsc(meetingId).stream()
				.filter(p -> p.getSpeakerIndex() != null && p.getNickname() != null)
				.collect(Collectors.toMap(Participant::getSpeakerIndex, Participant::getNickname,
						(a, b) -> a));
	}

	private void assertMeetingExists(String meetingId) {
		if (!meetingRepository.existsById(meetingId)) {
			throw new BusinessException(HttpStatus.NOT_FOUND, "해당 회의를 찾을 수 없습니다.");
		}
	}
}
