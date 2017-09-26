package us.raddev.cyapass;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by roger.deutsch on 8/18/2016.
 */
public class SiteKey {

    @SerializedName("MaxLength")
    private int maxLength;
    @SerializedName("HasSpecialChars")
    private boolean hasSpecialChars;
    @SerializedName("HasUpperCase")
    private boolean hasUpperCase;

    @SerializedName("Key")
    private String key;

    public SiteKey(String _name) {
        this.key = _name;
    }

    public static void toJSON(List<SiteKey> sk){
        Gson gson = new Gson();
        Log.d("MainActivity", "######################");
        Log.d("MainActivity", gson.toJson(sk));
    }

    @Override
    public String toString(){
        return key;
    }
}
