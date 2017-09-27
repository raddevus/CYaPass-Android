package us.raddev.cyapass;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by roger.deutsch on 8/18/2016.
 */
public class SiteKey {

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void setHasSpecialChars(boolean hasSpecialChars) {
        this.hasSpecialChars = hasSpecialChars;
    }

    public void setHasUpperCase(boolean hasUpperCase) {
        this.hasUpperCase = hasUpperCase;
    }

    @SerializedName("MaxLength")
    private int maxLength;
    @SerializedName("HasSpecialChars")
    private boolean hasSpecialChars;

    public int getMaxLength() {
        return maxLength;
    }

    public boolean isHasSpecialChars() {
        return hasSpecialChars;
    }

    public boolean isHasUpperCase() {
        return hasUpperCase;
    }

    public String getKey() {
        return key;
    }

    @SerializedName("HasUpperCase")
    private boolean hasUpperCase;

    @SerializedName("Key")
    private String key;

    public SiteKey(String key) {
        this.key = key;
    }

    public SiteKey(String key, boolean hasSpecialChars,
                   boolean hasUpperCase,
                   boolean hasMaxLength,
                   int maxLength)
    {
        this.key = key;
        this.hasSpecialChars = hasSpecialChars;
        this.hasUpperCase = hasUpperCase;
        this.maxLength = maxLength;
    }

    public static String toJson(List<SiteKey> sk){
        Gson gson = new Gson();
        Log.d("MainActivity", "######################");
        Log.d("MainActivity", gson.toJson(sk));
        return gson.toJson(sk);
    }

    @Override
    public String toString(){
        return key;
    }
}
