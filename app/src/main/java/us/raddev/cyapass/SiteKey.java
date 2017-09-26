package us.raddev.cyapass;

/**
 * Created by roger.deutsch on 8/18/2016.
 */
public class SiteKey {
    private String _name;

    public SiteKey(String _name) {
        this._name = _name;
    }

    @Override
    public String toString(){
        return _name;
    }
}
