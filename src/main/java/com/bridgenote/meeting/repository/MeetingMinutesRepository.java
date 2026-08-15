package com.bridgenote.meeting.repository;

import com.bridgenote.meeting.domain.MeetingMinutes;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, String> {
}
