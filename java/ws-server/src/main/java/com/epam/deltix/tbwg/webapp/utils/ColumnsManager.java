package com.epam.deltix.tbwg.webapp.utils;

import com.epam.deltix.tbwg.webapp.model.schema.TypeDef;
import com.epam.deltix.timebase.api.messages.service.ConnectionStatusChangeMessage;
import com.epam.deltix.timebase.api.messages.universal.PackageHeader;

/**
 * Created by Alex Karpovich on 21/08/2019.
 */
public class ColumnsManager {

    public static void applyDefaults(TypeDef type) {

        if (PackageHeader.CLASS_NAME.equals(type.name)) {
            for (int i = 0; i < type.fields.length; i++)
                type.fields[i].hidden = true;

            type.setVisible("packageType");
            type.setVisible("entries");
        } else if (ConnectionStatusChangeMessage.CLASS_NAME.equals(type.name)) {
            for (int i = 0; i < type.fields.length; i++)
                type.fields[i].hidden = true;
        }
    }
}
