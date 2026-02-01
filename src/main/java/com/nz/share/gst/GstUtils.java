package com.nz.share.gst;

import lombok.experimental.UtilityClass;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Pipeline;

@UtilityClass
public class GstUtils {

    private static Element mustMake(String factory, String name) {
        Element e = ElementFactory.make(factory, name);
        if (e == null) throw new IllegalStateException("Missing plugin: " + factory);
        return e;
    }
    private static void mustLink(Element... elements) {
        if (!Element.linkMany(elements))
            throw new IllegalStateException("Link failed near: " + elements[elements.length - 1].getName());
    }

    public static Pipeline getPipeline(Element el){
        Pipeline p;

        if (el instanceof Pipeline) {
            p = (Pipeline) el;
        } else {
            p = new Pipeline("p");
            p.add(el);                          // ajoute le Bin/Element au pipeline
            // pas besoin de plus : les éléments suivent l’état du parent
        }
        return p;
    }
}
