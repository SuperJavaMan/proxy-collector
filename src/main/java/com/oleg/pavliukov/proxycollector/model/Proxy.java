package com.oleg.pavliukov.proxycollector.model;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class Proxy implements Comparable<Proxy> {
    private Long quality;
    @NonNull
    private String ip;
    @NonNull
    private Integer port;
    private boolean isActive;

    @Override
    public int compareTo(Proxy o) {
        if (!isActive) {
            return 1;
        } if (!o.isActive) {
            return -1;
        }

        if (quality.intValue() >= o.quality) {
            return 1;
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Proxy proxy = (Proxy) o;

        if (!ip.equals(proxy.ip)) return false;
        return port.equals(proxy.port);
    }

    @Override
    public int hashCode() {
        int result = ip.hashCode();
        result = 31 * result + port.hashCode();
        return result;
    }
}
