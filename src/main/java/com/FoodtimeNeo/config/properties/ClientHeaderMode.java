package com.FoodtimeNeo.config.properties;

/**
 * How the client-identification header is treated on the protected paths.
 * <p>
 * {@code OFF} ignores it, {@code OBSERVE} logs unknown clients but lets them through so real-client
 * coverage can be measured before rollout, and {@code ENFORCE} rejects them.
 */
public enum ClientHeaderMode {
    OFF, OBSERVE, ENFORCE
}
