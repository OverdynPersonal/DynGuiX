package xyz.overdyn.dyngui.abstracts.title;

import net.kyori.adventure.text.Component;
import xyz.overdyn.dyngui.policy.GuiPolicy;

public interface AbstractGuiTitle {

    Component title();
    String getTitle();

    static AbstractGuiTitle title(Component title) {
        return new AbstractGuiTitle() {
            @Override
            public Component title() {
                return title;
            }

            @Override
            public String getTitle() {
                return null;
            }
        };
    }

    static AbstractGuiTitle title(String title) {
        return new AbstractGuiTitle() {
            @Override
            public Component title() {
                return null;
            }

            @Override
            public String getTitle() {
                return title;
            }
        };
    }

}
