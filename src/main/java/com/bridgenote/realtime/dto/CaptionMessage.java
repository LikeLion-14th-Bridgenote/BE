package com.bridgenote.realtime.dto;

/**
 * 서버 → 클라이언트 자막 메시지. (JSON snake_case: sentence_id, speaker_index, source_lang, source_text, is_final)
 */
public record CaptionMessage(
		String type,
		String sentenceId,
		Integer speakerIndex,
		String sourceLang,
		String sourceText,
		boolean isFinal
) {
	public static CaptionMessage of(String sentenceId, Integer speakerIndex,
									String sourceLang, String sourceText, boolean isFinal) {
		return new CaptionMessage("caption", sentenceId, speakerIndex, sourceLang, sourceText, isFinal);
	}
}
