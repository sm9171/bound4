package com.bound4.image.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("재시도 전략 테스트")
class RetryStrategyTest {

    @Test
    @DisplayName("기본 재시도 전략 생성")
    void defaultRetryStrategy() {
        // When
        RetryStrategy strategy = new RetryStrategy();

        // Then
        assertThat(strategy.getMaxRetryCount()).isEqualTo(5);
        assertThat(strategy.getBaseDelay()).isEqualTo(Duration.ofSeconds(2));
        assertThat(strategy.getBackoffMultiplier()).isEqualTo(2.0);
        assertThat(strategy.getMaxDelay()).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("재시도 가능 여부 확인")
    void canRetry() {
        // Given
        RetryStrategy strategy = new RetryStrategy();

        // When & Then
        assertThat(strategy.canRetry(0)).isTrue();
        assertThat(strategy.canRetry(4)).isTrue();
        assertThat(strategy.canRetry(5)).isFalse();
        assertThat(strategy.canRetry(10)).isFalse();
    }

    @Test
    @DisplayName("지수 백오프 지연 시간 계산")
    void calculateDelay() {
        // Given
        RetryStrategy strategy = new RetryStrategy(3, Duration.ofSeconds(1), 2.0, Duration.ofMinutes(1));

        // When & Then
        assertThat(strategy.calculateDelay(0)).isEqualTo(Duration.ofSeconds(1));
        assertThat(strategy.calculateDelay(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(strategy.calculateDelay(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(strategy.calculateDelay(3)).isEqualTo(Duration.ofSeconds(8));
    }

    @Test
    @DisplayName("최대 지연 시간 제한")
    void calculateDelay_MaxDelayLimit() {
        // Given
        RetryStrategy strategy = new RetryStrategy(10, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5));

        // When & Then
        assertThat(strategy.calculateDelay(0)).isEqualTo(Duration.ofSeconds(1));
        assertThat(strategy.calculateDelay(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(strategy.calculateDelay(2)).isEqualTo(Duration.ofSeconds(4));
        assertThat(strategy.calculateDelay(3)).isEqualTo(Duration.ofSeconds(5)); // maxDelay 제한
        assertThat(strategy.calculateDelay(10)).isEqualTo(Duration.ofSeconds(5)); // maxDelay 제한
    }

    @Test
    @DisplayName("지연 시간을 밀리초로 계산")
    void calculateDelayMillis() {
        // Given
        RetryStrategy strategy = new RetryStrategy(3, Duration.ofSeconds(1), 2.0, Duration.ofMinutes(1));

        // When & Then
        assertThat(strategy.calculateDelayMillis(0)).isEqualTo(1000);
        assertThat(strategy.calculateDelayMillis(1)).isEqualTo(2000);
        assertThat(strategy.calculateDelayMillis(2)).isEqualTo(4000);
    }

    @Test
    @DisplayName("잘못된 매개변수로 생성 시 예외")
    void invalidParameters() {
        // When & Then
        assertThatThrownBy(() -> new RetryStrategy(-1, Duration.ofSeconds(1), 2.0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max retry count must be non-negative");

        assertThatThrownBy(() -> new RetryStrategy(3, Duration.ZERO, 2.0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Base delay must be positive");

        assertThatThrownBy(() -> new RetryStrategy(3, Duration.ofSeconds(1), 1.0, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Backoff multiplier must be greater than 1.0");

        assertThatThrownBy(() -> new RetryStrategy(3, Duration.ofSeconds(10), 2.0, Duration.ofSeconds(5)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Max delay must be greater than or equal to base delay");
    }

    @Test
    @DisplayName("음수 재시도 횟수로 지연 시간 계산 시 예외")
    void calculateDelay_NegativeRetryCount() {
        // Given
        RetryStrategy strategy = new RetryStrategy();

        // When & Then
        assertThatThrownBy(() -> strategy.calculateDelay(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Retry count must be non-negative");
    }
}