package com.personalai.os.memory;

import com.personalai.os.entity.MemoryFact;
import com.personalai.os.entity.MemoryGoal;
import com.personalai.os.entity.MemoryTimeline;
import com.personalai.os.entity.MemoryTodo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * @description: 记忆生命周期服务类，管理记忆的强化、衰减、排序和状态迁移
 * @author: 琦
 */
@Service
public class MemoryLifecycleService {

    public enum MemoryType {
        NAME("Name"),
        BIRTHDAY("Birthday"),
        MAJOR("Major"),
        SCHOOL("School"),
        GOAL("Goal"),
        TIMELINE("Timeline"),
        TODO("Todo"),
        PREFERENCE("Preference"),
        OTHER("Other");

        private final String value;

        MemoryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public enum MemoryStatus {
        ACTIVE,
        DORMANT,
        ARCHIVED,
        DELETED
    }

    private static final double DORMANT_THRESHOLD = 0.3;
    private static final double ARCHIVED_THRESHOLD = 0.1;

    private static final int DEFAULT_DORMANT_DAYS = 30;
    private static final int DEFAULT_ARCHIVED_DAYS = 90;
    private static final int DEFAULT_DELETED_DAYS = 365;

    public double calculateRank(MemoryFact fact) {
        return calculateRank(
                fact.getImportance(),
                fact.getConfidence(),
                fact.getLastAccessTime(),
                fact.getAccessCount(),
                fact.getMemoryType()
        );
    }

    public double calculateRank(MemoryGoal goal) {
        return calculateRank(
                goal.getImportance(),
                goal.getConfidence(),
                goal.getLastAccessTime(),
                goal.getAccessCount(),
                goal.getMemoryType()
        );
    }

    public double calculateRank(MemoryTimeline timeline) {
        return calculateRank(
                timeline.getImportance(),
                timeline.getConfidence(),
                timeline.getLastAccessTime(),
                timeline.getAccessCount(),
                timeline.getMemoryType()
        );
    }

    public double calculateRank(MemoryTodo todo) {
        return calculateRank(
                todo.getImportance(),
                todo.getConfidence(),
                todo.getLastAccessTime(),
                todo.getAccessCount(),
                todo.getMemoryType()
        );
    }

    private double calculateRank(Integer importance, Integer confidence, 
                                  LocalDateTime lastAccessTime, Integer accessCount, 
                                  String memoryType) {
        double importanceScore = importance != null ? importance / 100.0 : 0.5;
        double confidenceScore = confidence != null ? confidence / 100.0 : 0.5;
        double decay = calculateDecay(lastAccessTime, memoryType);
        double accessWeight = calculateAccessWeight(accessCount);

        return importanceScore * confidenceScore * decay * accessWeight;
    }

    private double calculateDecay(LocalDateTime lastAccessTime, String memoryType) {
        if (lastAccessTime == null) {
            return 1.0;
        }

        long daysSinceLastAccess = ChronoUnit.DAYS.between(lastAccessTime, LocalDateTime.now());
        double decayRate = getDecayRateByType(memoryType);

        return Math.pow(decayRate, daysSinceLastAccess);
    }

    private double calculateAccessWeight(Integer accessCount) {
        int count = accessCount != null ? accessCount : 0;
        return 1.0 + Math.log(1 + count) / Math.log(10);
    }

    private double getDecayRateByType(String memoryType) {
        if (memoryType == null) {
            return 0.95;
        }

        return switch (memoryType) {
            case "Name", "Birthday" -> 1.0;
            case "Major", "School" -> 0.995;
            case "Goal" -> 0.98;
            case "Preference" -> 0.95;
            case "Timeline" -> 0.90;
            case "Todo" -> 0.85;
            default -> 0.95;
        };
    }

    public MemoryStatus determineStatus(double rank) {
        if (rank >= DORMANT_THRESHOLD) {
            return MemoryStatus.ACTIVE;
        } else if (rank >= ARCHIVED_THRESHOLD) {
            return MemoryStatus.DORMANT;
        } else {
            return MemoryStatus.ARCHIVED;
        }
    }

    public MemoryType inferTypeFromKey(String key) {
        if (key == null) {
            return MemoryType.OTHER;
        }

        String lowerKey = key.toLowerCase();
        if (lowerKey.contains("姓名") || lowerKey.contains("名字") || lowerKey.contains("name")) {
            return MemoryType.NAME;
        } else if (lowerKey.contains("生日") || lowerKey.contains("birth") || lowerKey.contains("年龄")) {
            return MemoryType.BIRTHDAY;
        } else if (lowerKey.contains("专业") || lowerKey.contains("major")) {
            return MemoryType.MAJOR;
        } else if (lowerKey.contains("学校") || lowerKey.contains("school")) {
            return MemoryType.SCHOOL;
        } else if (lowerKey.contains("目标") || lowerKey.contains("goal")) {
            return MemoryType.GOAL;
        } else if (lowerKey.contains("爱好") || lowerKey.contains("偏好") || lowerKey.contains("preference")) {
            return MemoryType.PREFERENCE;
        }
        return MemoryType.OTHER;
    }

    public int getDormantDaysThreshold(String memoryType) {
        if (memoryType == null) {
            return DEFAULT_DORMANT_DAYS;
        }

        return switch (memoryType) {
            case "Todo" -> 7;
            case "Timeline" -> 14;
            case "Goal" -> 15;
            case "Preference" -> 30;
            case "Major", "School" -> 60;
            case "Name", "Birthday" -> Integer.MAX_VALUE;
            default -> DEFAULT_DORMANT_DAYS;
        };
    }

    public int getArchivedDaysThreshold(String memoryType) {
        if (memoryType == null) {
            return DEFAULT_ARCHIVED_DAYS;
        }

        return switch (memoryType) {
            case "Todo" -> 30;
            case "Timeline" -> 60;
            case "Goal" -> 45;
            case "Preference" -> 90;
            case "Major", "School" -> 180;
            case "Name", "Birthday" -> Integer.MAX_VALUE;
            default -> DEFAULT_ARCHIVED_DAYS;
        };
    }

    public int getDeletedDaysThreshold(String memoryType) {
        if (memoryType == null) {
            return DEFAULT_DELETED_DAYS;
        }

        return switch (memoryType) {
            case "Todo" -> 180;
            case "Timeline" -> 365;
            case "Goal" -> 365;
            case "Preference" -> 365;
            case "Major", "School" -> 730;
            case "Name", "Birthday" -> Integer.MAX_VALUE;
            default -> DEFAULT_DELETED_DAYS;
        };
    }

    public boolean isDecayApplicable(String memoryType) {
        return !"Name".equals(memoryType) && !"Birthday".equals(memoryType);
    }

    public int reinforceImportance(Integer currentImportance, int delta) {
        return Math.min(100, Math.max(0, (currentImportance != null ? currentImportance : 50) + delta));
    }

    public int reinforceConfidence(Integer currentConfidence, int delta) {
        return Math.min(100, Math.max(0, (currentConfidence != null ? currentConfidence : 0) + delta));
    }

    public static final int REINFORCE_ACCESS = 2;
    public static final int REINFORCE_MENTION_IMPORTANCE = 3;
    public static final int REINFORCE_MENTION_CONFIDENCE = 1;
    public static final int REINFORCE_GOAL_COMPLETE = 5;
    public static final int REINFORCE_TIMELINE_REF = 3;
}