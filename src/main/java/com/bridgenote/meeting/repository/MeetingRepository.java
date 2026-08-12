package com.bridgenote.meeting.repository;

import com.bridgenote.meeting.domain.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, String> {

	/** 초대 코드 중복 방지용. */
	boolean existsByInviteCode(String inviteCode);

	/**
	 * 로그인 사용자가 주최했거나(host) 참가한(participant) 회의 목록. 최신순.
	 */
	@Query("""
			SELECT m FROM Meeting m
			WHERE m.hostId = :userId
			   OR EXISTS (SELECT 1 FROM Participant p
			              WHERE p.meetingId = m.id AND p.profileId = :userId)
			ORDER BY m.createdAt DESC
			""")
	List<Meeting> findMyMeetings(@Param("userId") String userId);
}
