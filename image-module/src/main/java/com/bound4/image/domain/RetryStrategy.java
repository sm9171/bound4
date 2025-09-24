package com.bound4.image.domain;

import java.time.Duration;

/**
 * 재시도 전략을 정의하는 값 객체
 */
public class RetryStrategy {
    
    private static final int MAX_RETRY_COUNT = 5;
    private static final Duration BASE_DELAY = Duration.ofSeconds(2);
    private static final double BACKOFF_MULTIPLIER = 2.0;
    private static final Duration MAX_DELAY = Duration.ofMinutes(5);
    
    private final int maxRetryCount;
    private final Duration baseDelay;
    private final double backoffMultiplier;
    private final Duration maxDelay;
    
    public RetryStrategy() {
        this(MAX_RETRY_COUNT, BASE_DELAY, BACKOFF_MULTIPLIER, MAX_DELAY);
    }
    
    public RetryStrategy(int maxRetryCount, Duration baseDelay, double backoffMultiplier, Duration maxDelay) {
        if (maxRetryCount < 0) {
            throw new IllegalArgumentException("Max retry count must be non-negative");
        }
        if (baseDelay.isNegative() || baseDelay.isZero()) {
            throw new IllegalArgumentException("Base delay must be positive");
        }
        if (backoffMultiplier <= 1.0) {
            throw new IllegalArgumentException("Backoff multiplier must be greater than 1.0");
        }
        if (maxDelay.compareTo(baseDelay) < 0) {
            throw new IllegalArgumentException("Max delay must be greater than or equal to base delay");
        }
        
        this.maxRetryCount = maxRetryCount;
        this.baseDelay = baseDelay;
        this.backoffMultiplier = backoffMultiplier;
        this.maxDelay = maxDelay;
    }
    
    /**
     * 재시도 가능 여부 확인
     * @param currentRetryCount 현재 재시도 횟수
     * @return 재시도 가능 여부
     */
    public boolean canRetry(int currentRetryCount) {
        return currentRetryCount < maxRetryCount;
    }
    
    /**
     * 다음 재시도까지의 지연 시간 계산 (지수 백오프)
     * @param retryCount 재시도 횟수 (0부터 시작)
     * @return 지연 시간
     */
    public Duration calculateDelay(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("Retry count must be non-negative");
        }
        
        if (retryCount == 0) {
            return baseDelay;
        }
        
        // 지수 백오프: baseDelay * (backoffMultiplier ^ retryCount)
        double delaySeconds = baseDelay.toMillis() * Math.pow(backoffMultiplier, retryCount) / 1000.0;
        Duration calculatedDelay = Duration.ofMillis((long) (delaySeconds * 1000));
        
        // 최대 지연 시간 제한
        return calculatedDelay.compareTo(maxDelay) > 0 ? maxDelay : calculatedDelay;
    }
    
    /**
     * 다음 재시도 시간 계산
     * @param retryCount 재시도 횟수
     * @return 다음 재시도까지의 지연 시간 (밀리초)
     */
    public long calculateDelayMillis(int retryCount) {
        return calculateDelay(retryCount).toMillis();
    }
    
    public int getMaxRetryCount() {
        return maxRetryCount;
    }
    
    public Duration getBaseDelay() {
        return baseDelay;
    }
    
    public double getBackoffMultiplier() {
        return backoffMultiplier;
    }
    
    public Duration getMaxDelay() {
        return maxDelay;
    }
    
    @Override
    public String toString() {
        return "RetryStrategy{" +
                "maxRetryCount=" + maxRetryCount +
                ", baseDelay=" + baseDelay +
                ", backoffMultiplier=" + backoffMultiplier +
                ", maxDelay=" + maxDelay +
                '}';
    }
}