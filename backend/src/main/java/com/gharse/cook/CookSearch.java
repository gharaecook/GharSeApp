package com.gharse.cook;

import com.gharse.menu.DishCategory;
import java.math.BigDecimal;

/** Discovery filters. Every field is optional. */
public record CookSearch(String q, String cuisine, DishCategory category, Boolean vegetarian, BigDecimal minRating,
                         Boolean availableToday, String locality) {}
