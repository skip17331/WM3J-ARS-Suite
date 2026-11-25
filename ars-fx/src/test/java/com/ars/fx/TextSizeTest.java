package com.ars.fx;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextSizeTest {
    @Test void scalesEveryFontSizeAndNothingElse() {
        String css = ".a{-fx-font-size:10px;-fx-padding:4px;} .b{-fx-font-size: 9.5px}";
        assertEquals(".a{-fx-font-size:15px;-fx-padding:4px;} .b{-fx-font-size: 14.5px}", TextSize.scaleCss(css, 1.5));
    }
    @Test void hundredPercentIsUntouched() {
        String css = ".a{-fx-font-size:11px}";
        assertEquals(css, TextSize.scaleCss(css, 1.0));
    }
}
