/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package paintbynumberpro;

import java.util.*;
import java.awt.*;

/**
 *
 * @author Lynne
 */
public class Settings {

    public final static int NUM_GUESS_COLORS = 8;
    private final String GuessSettingKeyword = "guess_color";

    // List of possible keywords we might know about
    public static enum SettingKeyword {
        PUZZLE_DIR,
        BOX_SIZE,
        CLUE_HEIGHT,
        CLUE_WIDTH,
        GUESS_COLOR_0,
        GUESS_COLOR_1,
        GUESS_COLOR_2,
        GUESS_COLOR_3,
        GUESS_COLOR_4,
        GUESS_COLOR_5,
        GUESS_COLOR_6,
        GUESS_COLOR_7,
        BACKGROUND_COLOR,
        FONT_SIZE
    }
    private String[] keywords = { "puzzle_dir",
        "box_size", "clue_height", "clue_width",
        GuessSettingKeyword + "0", GuessSettingKeyword + "1",
        GuessSettingKeyword + "2", GuessSettingKeyword + "3",
        GuessSettingKeyword + "4", GuessSettingKeyword + "5",
        GuessSettingKeyword + "6", GuessSettingKeyword + "7",
        "background_color",
        "font_size"
    };
    private String[] default_values = { null,
        "19", // box size
        "13", // clue height
        "15", // clue width
        "255,0,0", "0,255,0",       // red, green
        "255,200,0", "255,0,255",   // orange, magenta
        "255,255,0", "244,138,102", // yellow, salmon
        "192,192,192", "0,255,255", // lt gray, cyan
        "255,255,255",              // white
        "10"                        // font size
    };

    // HashMap of keyword / value pairs stored in preferences
    private HashMap hm = null;

    Settings ()
    {
        if (!ReadSettings())
        {
            hm = new HashMap();
            int num_keywords = keywords.length;
            for (int i=0; i<num_keywords; i++)
                hm.put (keywords[i], default_values[i]);
        }
    }

    public String GetSettingFor (SettingKeyword pk)
    {
        if (hm == null) return null;
        String keyword = keywords[pk.ordinal()];
        return (String)hm.get(keyword);
    }

    public int GetIntegerSettingFor (SettingKeyword pk)
    {
        if (hm == null) return 0;
        int ival = 0;
        String keyword = keywords[pk.ordinal()];
        String value = (String)hm.get(keyword);
        if (value != null)
        {
            try { ival = Integer.parseInt (value); }
            catch (NumberFormatException nfe) { }
        }
        return ival;
    }

    public Color GetBackgroundColor ()
    {
        Color c = Color.WHITE;
        if (hm == null) return c;
        String valueStr = (String)hm.get(keywords[SettingKeyword.BACKGROUND_COLOR.ordinal()]);
        if (valueStr != null) c = ParseColorFromValue (valueStr);
        return c;
    }

    public Color GetGuessColorSettingFor (int guess_col)
    {
        Color c = Color.RED;
        if (hm == null) return c;
        String color_keyword = GuessSettingKeyword + Integer.toString(guess_col);
        for (String key : keywords)
            if (key.startsWith (color_keyword) && key.length() == color_keyword.length())
            {
                String valueStr = (String)hm.get(color_keyword);
                if (valueStr != null)
                {
                    c = ParseColorFromValue (valueStr);
                    return c;
                }
            }
        return c;
    }

    private static Color ParseColorFromValue (String valueStr)
    {
        Color c = null;
        if (valueStr != null && valueStr.trim().length() > 4)
        {
            String redStr, blueStr, greenStr;
            int indx = valueStr.indexOf (",");
            if (indx <= 0) return c;
            redStr = valueStr.substring (0, indx);
            valueStr = valueStr.substring (indx+1);
            indx = valueStr.indexOf (",");
            if (indx <= 0) return c;
            greenStr = valueStr.substring (0, indx);
            blueStr = valueStr.substring (indx+1);

            int r, g, b;
            try
            {
                r = Integer.parseInt (redStr);
                g = Integer.parseInt (greenStr);
                b = Integer.parseInt (blueStr);
                c = new Color (r, g, b);
            }
            catch (NumberFormatException nfe) { return c; }
            return c;
        }
        return c;
    }

    public void SetSettingFor (SettingKeyword pk, String value)
    {
        if (hm == null) hm = new HashMap();
        hm.put (keywords[pk.ordinal()], value);
    }

    public boolean SaveSettings ()
    {
        System.out.println ("Preferences.SaveSettings() NOT YET IMPLEMENTED");
        return true;
    }

    public boolean ReadSettings ()
    {
        System.out.println ("Preferences.ReadSettings() NOT YET IMPLEMENTED");
        return false;
    }

}
