package com.battlesnake;

import com.battlesnake.data.Snake;

public class SnakeLength {
    public static boolean isLonger(Snake a, Snake b) {
        return a.getCoords().length > b.getCoords().length;
    }
}
