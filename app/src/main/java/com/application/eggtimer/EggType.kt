package com.application.eggtimer

enum class EggType(val title: String, val seconds: Int, val drawableId: Int, val description: String) {
    SOFT("Soft", 300, R.drawable.soft_boiled_egg, "Runny yolk and set whites. Perfect for dipping toast soldiers."),
    MEDIUM("Medium", 420, R.drawable.medium_boiled_egg, "Jammy, custardy yolk. Great for ramen or salads."),
    HARD("Hard", 600, R.drawable.hard_boiled_egg, "Fully set yolk and whites. Ideal for egg salad or snacks.")
}