/*******************************************************************************
 *
 *  Copyright FUJITSU LIMITED 2018
 *
 *******************************************************************************/
package org.oscm.app.ui.platformconfiguration;

import org.oscm.app.ui.BaseModel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import java.util.List;
import java.util.TreeMap;

/**
 * Created by PLGrubskiM on 2017-04-24.
 */
@ViewScoped
@ManagedBean
public class PlatformConfigurationModel extends BaseModel {

    private List<String> keys;
    private TreeMap<String, String> items;


    public TreeMap<String, String> getItems() {
        return items;
    }

    public void setItems(TreeMap<String, String> items) {
        this.items = items;
    }

    public List<String> getKeys() {
        return keys;
    }

    public void setKeys(List<String> keys) {
        this.keys = keys;
    }
}
