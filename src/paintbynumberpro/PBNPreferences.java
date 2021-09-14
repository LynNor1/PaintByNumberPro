/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package paintbynumberpro;

import java.util.prefs.Preferences;

/**
 *
 * @author Lynne
 */
public enum PBNPreferences {
    INSTANCE;

    private Preferences myPrefs = null;
    private static String PBNLastSavedFile_Keyword = "PBN_last_saved_file";

    PBNPreferences ()
    {
        myPrefs = Preferences.userNodeForPackage(getClass());
    }
    
    public Preferences GetMyPreferences ()
    { return myPrefs; }
    
    public void SetLastSavedFile (String filename)
    {
        myPrefs.put(PBNLastSavedFile_Keyword, filename);
    }
    
    public String GetLastSavedFile ()
    {
        String lastSaved = myPrefs.get(PBNLastSavedFile_Keyword, null);
        return lastSaved;
    }
}
