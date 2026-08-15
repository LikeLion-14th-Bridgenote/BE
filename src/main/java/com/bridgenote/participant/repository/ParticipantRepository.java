package com.bridgenote.participant.repository;

import com.bridgenote.participant.domain.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, String> {

	/**
	 * 여러 회의의 "입장(join)한" 참가자 수를 한 번에 집계. 반환: [meetingId, count] 행 목록.
	 */
	@Query("""
			SELECT p.meetingId, COUNT(p) FROM Participant p
			WHERE p.meetingId IN :meetingIds AND p.speakerIndex IS NOT NULL
			GROUP BY p.meetingId
			""")
	List<Object[]> countJoinedGroupedByMeeting(@Param("meetingIds") Collection<String> meetingIds);

	/** 특정 회의의 입장한 참가자 목록 (화자번호 순). */
	List<Participant> findByMeetingIdAndSpeakerIndexIsNotNullOrderBySpeakerIndexAsc(String meetingId);

	/** 중복 참가/동의 여부 확인용. */
	Optional<Participant> findByMeetingIdAndProfileId(String meetingId, String profileId);

	/** 화자 번호 배정용(현재 입장한 참가자 수). */
	long countByMeetingIdAndSpeakerIndexIsNotNull(String meetingId);
}
