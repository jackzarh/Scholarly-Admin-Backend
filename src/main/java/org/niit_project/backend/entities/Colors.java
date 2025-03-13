package org.niit_project.backend.entities;

import java.util.Random;

public enum Colors {
    red,
    blue,
    green,
    purple,
    magenta,
    orange,
    darkviolet,
    indigo,
    palevioletred,
    peru,
    goldenrod,
    tomato,
    teal,
    slateblue,
    sienna,
    seagreen,
    royalblue,
    olive,
    olivedrab,
    midnightblue,
    orangered,
    mediumvioletred,
    chocolate,
    crimson,
    brown;

    public static Colors getRandomColor(){
        var totalColors = values().length;

        var randomIndex = new Random().nextInt(totalColors);

        return Colors.values()[randomIndex];
    }
}


