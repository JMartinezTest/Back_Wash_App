package com.proyecto.san_felipe.config;

import com.proyecto.san_felipe.entities.WashRecord;
import org.bson.Document;
import org.springframework.data.mongodb.core.mapping.event.AbstractMongoEventListener;
import org.springframework.data.mongodb.core.mapping.event.AfterLoadEvent;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WashRecordEventListener extends AbstractMongoEventListener<WashRecord> {

    @Override
    public void onAfterLoad(AfterLoadEvent<WashRecord> event) {
        Document doc = event.getDocument();
        if (doc == null) return;

        Object so = doc.get("serviceOffered");
        // Si el campo es un String (documentos viejos), convertirlo a List
        if (so instanceof String) {
            doc.put("serviceOffered", List.of((String) so));
        }
    }
}
