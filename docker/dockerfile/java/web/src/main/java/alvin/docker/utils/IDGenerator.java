package alvin.docker.utils;

import lombok.val;
import lombok.var;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class IDGenerator {
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Times times;

    public IDGenerator(Times times) {
        this.times = times;
    }

    public String randomUUID(String prefix, String splitter) {
        var uuid = UUID.randomUUID().toString();
        if (!"-".equals(splitter)) {
            uuid = uuid.replace("-", splitter).toUpperCase();
        }
        return prefix + uuid;
    }

    public String randomUUID(String prefix) {
        return randomUUID(prefix, "");
    }

    public String serialNumber(String prefix, long number, ZoneId zone) {
        val dt = Times.utcToZone(times.utcNow(), zone);
        return prefix + dt.format(dateTimeFormatter) + String.format("%04d", number);
    }
}
