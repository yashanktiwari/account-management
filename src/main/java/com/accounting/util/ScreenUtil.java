package com.accounting.util;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class ScreenUtil {

    public static class DialogSize {
        public final double width;
        public final double height;

        public DialogSize(double width, double height) {
            this.width = width;
            this.height = height;
        }
    }

    /**
     * Calculate appropriate dialog size based on screen resolution.
     * Ensures dialogs fit within 90% of screen height and 95% of screen width.
     *
     * @param preferredWidth  Preferred width for larger screens
     * @param preferredHeight Preferred height for larger screens
     * @return DialogSize with adjusted dimensions
     */
    public static DialogSize getResponsiveSize(double preferredWidth, double preferredHeight) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        
        double maxWidth = screenBounds.getWidth() * 0.95;
        double maxHeight = screenBounds.getHeight() * 0.90;
        
        double finalWidth = Math.min(preferredWidth, maxWidth);
        double finalHeight = Math.min(preferredHeight, maxHeight);
        
        return new DialogSize(finalWidth, finalHeight);
    }

    /**
     * Get screen height for responsive calculations
     */
    public static double getScreenHeight() {
        return Screen.getPrimary().getVisualBounds().getHeight();
    }

    /**
     * Get screen width for responsive calculations
     */
    public static double getScreenWidth() {
        return Screen.getPrimary().getVisualBounds().getWidth();
    }

    /**
     * Check if screen is small (height < 900px)
     */
    public static boolean isSmallScreen() {
        return getScreenHeight() < 900;
    }
}
