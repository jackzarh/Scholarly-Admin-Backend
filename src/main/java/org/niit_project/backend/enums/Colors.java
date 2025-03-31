package org.niit_project.backend.enums;

import java.util.Random;

public enum Colors {
    red,
    blue,
    green,
    purple,
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
    brown,

    // Not more used or planning to be removed:
    magenta;

    // Total enums being removed: 1

    public static Colors getRandomColor(){
        var totalColors = values().length -1;

        var randomIndex = new Random().nextInt(totalColors);

        return Colors.values()[randomIndex];
    }
}


