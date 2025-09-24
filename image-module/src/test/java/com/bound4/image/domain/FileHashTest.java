package com.bound4.image.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileHashTest {
    
    @Test
    void createFileHash_WithValidSHA256_Success() {
        // Given
        String validHash = "a".repeat(64);
        
        // When
        FileHash fileHash = FileHash.of(validHash);
        
        // Then
        assertThat(fileHash.value()).isEqualTo(validHash);
    }
    
    @Test
    void createFileHash_WithValidHexadecimalHash_Success() {
        // Given
        String validHash = "0123456789abcdef".repeat(4);
        
        // When
        FileHash fileHash = FileHash.of(validHash);
        
        // Then
        assertThat(fileHash.value()).isEqualTo(validHash);
    }
    
    @Test
    void createFileHash_WithNullValue_ThrowsException() {
        // Given
        String value = null;
        
        // When & Then
        assertThatThrownBy(() -> FileHash.of(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FileHash cannot be null or empty");
    }
    
    @Test
    void createFileHash_WithEmptyValue_ThrowsException() {
        // Given
        String value = "";
        
        // When & Then
        assertThatThrownBy(() -> FileHash.of(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FileHash cannot be null or empty");
    }
    
    @Test
    void createFileHash_WithWhitespaceValue_ThrowsException() {
        // Given
        String value = "   ";
        
        // When & Then
        assertThatThrownBy(() -> FileHash.of(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FileHash cannot be null or empty");
    }
    
    @Test
    void createFileHash_WithInvalidLength_ThrowsException() {
        // Given
        String value = "a".repeat(63); // Too short
        
        // When & Then
        assertThatThrownBy(() -> FileHash.of(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FileHash must be 64 characters (SHA-256)");
    }
    
    @Test
    void createFileHash_WithTooLongValue_ThrowsException() {
        // Given
        String value = "a".repeat(65); // Too long
        
        // When & Then
        assertThatThrownBy(() -> FileHash.of(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FileHash must be 64 characters (SHA-256)");
    }
    
    @Test
    void createFileHash_WithNonHexadecimalCharacters_ThrowsException() {
        // Given
        String value = "g".repeat(64); // 'g' is not hexadecimal
        
        // When & Then
        assertThatThrownBy(() -> FileHash.of(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FileHash must be hexadecimal");
    }
    
    @Test
    void createFileHash_WithSpecialCharacters_ThrowsException() {
        // Given
        String value = "a".repeat(63) + "!"; // Contains special character
        
        // When & Then
        assertThatThrownBy(() -> FileHash.of(value))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("FileHash must be hexadecimal");
    }
    
    @Test
    void equality_SameValue_ReturnsTrue() {
        // Given
        String hashValue = "a".repeat(64);
        FileHash fileHash1 = FileHash.of(hashValue);
        FileHash fileHash2 = FileHash.of(hashValue);
        
        // When & Then
        assertThat(fileHash1).isEqualTo(fileHash2);
        assertThat(fileHash1.hashCode()).isEqualTo(fileHash2.hashCode());
    }
    
    @Test
    void equality_DifferentValue_ReturnsFalse() {
        // Given
        FileHash fileHash1 = FileHash.of("a".repeat(64));
        FileHash fileHash2 = FileHash.of("b".repeat(64));
        
        // When & Then
        assertThat(fileHash1).isNotEqualTo(fileHash2);
    }
    
    @Test
    void caseInsensitive_UppercaseHex_Success() {
        // Given
        String uppercaseHash = "ABCDEF" + "0".repeat(58);
        
        // When
        FileHash fileHash = FileHash.of(uppercaseHash);
        
        // Then
        assertThat(fileHash.value()).isEqualTo(uppercaseHash);
    }
}