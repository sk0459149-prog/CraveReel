package com.recipereels.dto;

public class IngredientDTO {
    private Long id;
    private String name;
    private String quantity;
    private String unit;
    private Integer orderIndex;

    public IngredientDTO() {
    }

    public IngredientDTO(String name, String quantity, String unit, Integer orderIndex) {
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.orderIndex = orderIndex;
    }

    public IngredientDTO(Long id, String name, String quantity, String unit, Integer orderIndex) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.orderIndex = orderIndex;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }
}
