package com.recipereels.dto;

public class StepDTO {
    private Long id;
    private Integer stepNumber;
    private String title;
    private String instruction;
    private Integer timerMinutes;
    private String tip;

    public StepDTO() {
    }

    public StepDTO(Integer stepNumber, String title, String instruction, Integer timerMinutes, String tip) {
        this.stepNumber = stepNumber;
        this.title = title;
        this.instruction = instruction;
        this.timerMinutes = timerMinutes;
        this.tip = tip;
    }

    public StepDTO(Long id, Integer stepNumber, String title, String instruction, Integer timerMinutes, String tip) {
        this.id = id;
        this.stepNumber = stepNumber;
        this.title = title;
        this.instruction = instruction;
        this.timerMinutes = timerMinutes;
        this.tip = tip;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStepNumber() {
        return stepNumber;
    }

    public void setStepNumber(Integer stepNumber) {
        this.stepNumber = stepNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Integer getTimerMinutes() {
        return timerMinutes;
    }

    public void setTimerMinutes(Integer timerMinutes) {
        this.timerMinutes = timerMinutes;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }
}
