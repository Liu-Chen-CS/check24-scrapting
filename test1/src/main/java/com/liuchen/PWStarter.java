package com.liuchen;
import com.microsoft.playwright.*;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class PWStarter {

    private static Playwright playwright;

    private PWStarter(){
        playwright = Playwright.create();
    }

    public static Playwright getPlaywright() {
        if(playwright == null){
            new PWStarter();
        }
        return playwright;
    }

}
