package com.bridgenote.realtime.dto;

/**
 * 서버 → 클라이언트 번역 메시지. (JSON snake_case: sentence_id, target_lang)
 */
public record TranslationMessage(
		String type,
		String sentenceId,
		String targetLang,
		String text
) {
	public static TranslationMessage of(String sentenceId, String targetLang, String text) {
		return new TranslationMessage("translation", sentenceId, targetLang, text);
	}
}
