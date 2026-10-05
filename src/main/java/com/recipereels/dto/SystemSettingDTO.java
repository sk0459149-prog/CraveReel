package com.recipereels.dto;

public class SystemSettingDTO {
    private String websiteName = "RecipeReels";
    private Integer maxVideoSizeMb = 100;
    private Integer maxReelDurationSec = 90;
    private boolean allowComments = true;
    private boolean allowRatings = true;
    private boolean allowNewContributors = true;
    private String contentModerationMode = "MANUAL_APPROVAL"; // MANUAL_APPROVAL or AUTO_APPROVE

    public SystemSettingDTO() {
    }

    public String getWebsiteName() {
        return websiteName;
    }

    public void setWebsiteName(String websiteName) {
        this.websiteName = websiteName;
    }

    public Integer getMaxVideoSizeMb() {
        return maxVideoSizeMb;
    }

    public void setMaxVideoSizeMb(Integer maxVideoSizeMb) {
        this.maxVideoSizeMb = maxVideoSizeMb;
    }

    public Integer getMaxReelDurationSec() {
        return maxReelDurationSec;
    }

    public void setMaxReelDurationSec(Integer maxReelDurationSec) {
        this.maxReelDurationSec = maxReelDurationSec;
    }

    public boolean isAllowComments() {
        return allowComments;
    }

    public void setAllowComments(boolean allowComments) {
        this.allowComments = allowComments;
    }

    public boolean isAllowRatings() {
        return allowRatings;
    }

    public void setAllowRatings(boolean allowRatings) {
        this.allowRatings = allowRatings;
    }

    public boolean isAllowNewContributors() {
        return allowNewContributors;
    }

    public void setAllowNewContributors(boolean allowNewContributors) {
        this.allowNewContributors = allowNewContributors;
    }

    public String getContentModerationMode() {
        return contentModerationMode;
    }

    public void setContentModerationMode(String contentModerationMode) {
        this.contentModerationMode = contentModerationMode;
    }
}
