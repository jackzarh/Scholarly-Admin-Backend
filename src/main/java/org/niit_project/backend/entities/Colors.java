package org.niit_project.backend.entities;

import java.util.Random;

public enum Colors {
    red,
    yellow,
    blue,
    green,
    purple,
    pink,
    orange,
    violet,
    indigo,
    peach,
    gold,
    brown;

    public static Colors getRandomColor(){
        var totalColors = values().length;

        var randomIndex = new Random().nextInt(totalColors);

        return Colors.values()[randomIndex];
    }
}


