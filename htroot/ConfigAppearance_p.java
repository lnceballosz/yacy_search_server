// ConfigSkins_p.java 
// -----------------------
// part of YACY
// (C) by Michael Peter Christen; mc@yacy.net
// first published on http://www.anomic.de
// Frankfurt, Germany, 2004
//
// This File is contributed by Alexander Schier
// last change: 29.12.2004
// extended by Michael Christen, 4.7.2008
//
// LICENSE
// 
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import de.anomic.crawler.HTTPLoader;
import de.anomic.data.listManager;
import de.anomic.http.httpClient;
import de.anomic.http.httpRequestHeader;
import de.anomic.kelondro.util.FileUtils;
import de.anomic.plasma.plasmaSwitchboard;
import de.anomic.plasma.plasmaSwitchboardConstants;
import de.anomic.server.serverObjects;
import de.anomic.server.serverSwitch;
import de.anomic.yacy.yacyURL;

public class ConfigAppearance_p {

	public static serverObjects respond(final httpRequestHeader header, final serverObjects post, final serverSwitch<?> env) {
        final serverObjects prop = new serverObjects();
        final plasmaSwitchboard sb = (plasmaSwitchboard) env;
        final String skinPath = new File(env.getRootPath(), env.getConfig("skinPath", "DATA/SKINS")).toString();

        // Fallback
        prop.put("currentskin", "");
        prop.put("status", "0"); // nothing

        List<String> skinFiles = listManager.getDirListing(skinPath);
        if (skinFiles == null) {
            return prop;
        }

        // if there are no skins, use the current style as default
        // normally only invoked at first start of YaCy
        if (skinFiles.size() == 0) {
            try {
                FileUtils.copy(new File(env.getRootPath(), "htroot/env/style.css"), new File(skinPath, "default.css"));
                env.setConfig("currentSkin", "default");
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        if (post != null) {
            if (post.containsKey("use_button") && post.get("skin") != null) {
                // change skin
                changeSkin(sb, skinPath, post.get("skin"));

            }
            if (post.containsKey("delete_button")) {
                // delete skin
                final File skinfile = new File(skinPath, post.get("skin"));
                FileUtils.deletedelete(skinfile);

            }
            if (post.containsKey("install_button")) {
                // load skin from URL
                final String url = post.get("url");
                ArrayList<String> skinVector;
                try {
                    final yacyURL u = new yacyURL(url, null);
                    final httpRequestHeader reqHeader = new httpRequestHeader();
                    reqHeader.put(httpRequestHeader.USER_AGENT, HTTPLoader.yacyUserAgent);
                    skinVector = FileUtils.strings(httpClient.wget(u.toString(), reqHeader, 10000), "UTF-8");
                } catch (final IOException e) {
                    prop.put("status", "1");// unable to get URL
                    prop.put("status_url", url);
                    return prop;
                }
                try {
                    final Iterator<String> it = skinVector.iterator();
                    final File skinFile = new File(skinPath, url.substring(url.lastIndexOf("/"), url.length()));
                    final BufferedWriter bw = new BufferedWriter(new PrintWriter(new FileWriter(skinFile)));

                    while (it.hasNext()) {
                        bw.write(it.next() + "\n");
                    }
                    bw.close();
                } catch (final IOException e) {
                    prop.put("status", "2");// error saving the skin
                    return prop;
                }
                if (post.containsKey("use_skin") && (post.get("use_skin", "")).equals("on")) {
                    changeSkin(sb, skinPath, url.substring(url.lastIndexOf("/"), url.length()));
                }
            }
            if (post.containsKey("searchpage_set")) {
                sb.setConfig(plasmaSwitchboardConstants.GREETING, post.get(plasmaSwitchboardConstants.GREETING, ""));
                sb.setConfig(plasmaSwitchboardConstants.GREETING_HOMEPAGE, post.get(plasmaSwitchboardConstants.GREETING_HOMEPAGE, ""));
                sb.setConfig(plasmaSwitchboardConstants.GREETING_LARGE_IMAGE, post.get(plasmaSwitchboardConstants.GREETING_LARGE_IMAGE, ""));
                sb.setConfig(plasmaSwitchboardConstants.GREETING_SMALL_IMAGE, post.get(plasmaSwitchboardConstants.GREETING_SMALL_IMAGE, ""));
            }
            if (post.containsKey("searchpage_default")) {
                sb.setConfig(plasmaSwitchboardConstants.GREETING, "P2P Web Search");
                sb.setConfig(plasmaSwitchboardConstants.GREETING_HOMEPAGE, "http://yacy.net");
                sb.setConfig(plasmaSwitchboardConstants.GREETING_LARGE_IMAGE, "/env/grafics/YaCyLogo_120ppi.png");
                sb.setConfig(plasmaSwitchboardConstants.GREETING_SMALL_IMAGE, "/env/grafics/YaCyLogo_60ppi.png");
            }
            
            if (post.containsKey("popup")) {
                String popup = post.get("popup", "status");
                if (popup.equals("front")) {
                    sb.setConfig(plasmaSwitchboardConstants.BROWSER_POP_UP_PAGE, "index.html?display=2");
                } else if (popup.equals("search")) {
                    sb.setConfig(plasmaSwitchboardConstants.BROWSER_POP_UP_PAGE, "yacysearch.html?display=2");
                } else if (popup.equals("interactive")) {
                    sb.setConfig(plasmaSwitchboardConstants.BROWSER_POP_UP_PAGE, "yacyinteractive.html?display=2");
                } else {
                    sb.setConfig(plasmaSwitchboardConstants.BROWSER_POP_UP_PAGE, "Status.html");
                }
            }
            
        }

        // reread skins
        skinFiles = listManager.getDirListing(skinPath);
        int count = 0;
        for (String skinFile : skinFiles) {
            if (skinFile.endsWith(".css")) {
                prop.put("skinlist_" + count + "_file", skinFile);
                prop.put("skinlist_" + count + "_name", skinFile.substring(0, skinFile.length() - 4));
                count++;
            }
        }
        prop.put("skinlist", count);

        prop.putHTML("currentskin", env.getConfig("currentSkin", "default"));
        
        prop.putHTML(plasmaSwitchboardConstants.GREETING, sb.getConfig(plasmaSwitchboardConstants.GREETING, ""));
        prop.putHTML(plasmaSwitchboardConstants.GREETING_HOMEPAGE, sb.getConfig(plasmaSwitchboardConstants.GREETING_HOMEPAGE, ""));
        prop.putHTML(plasmaSwitchboardConstants.GREETING_LARGE_IMAGE, sb.getConfig(plasmaSwitchboardConstants.GREETING_LARGE_IMAGE, ""));
        prop.putHTML(plasmaSwitchboardConstants.GREETING_SMALL_IMAGE, sb.getConfig(plasmaSwitchboardConstants.GREETING_SMALL_IMAGE, ""));
        
        final String  browserPopUpPage = sb.getConfig(plasmaSwitchboardConstants.BROWSER_POP_UP_PAGE, "ConfigBasic.html");
        prop.put("popupFront", 0);
        prop.put("popupSearch", 0);
        prop.put("popupInteractive", 0);
        prop.put("popupStatus", 0);
        if (browserPopUpPage.startsWith("index")) {
            prop.put("popupFront", 1);
        } else if (browserPopUpPage.startsWith("yacysearch")) {
            prop.put("popupSearch", 1);
        } else if (browserPopUpPage.startsWith("yacyinteractive")) {
            prop.put("popupInteractive", 1);
        } else {
            prop.put("popupStatus", 1);
        }
        
        String myaddress = sb.peers.mySeed().getPublicAddress();
        if (myaddress == null) myaddress = "localhost:" + sb.getConfig("port", "8080");
        prop.put("myaddress", myaddress);
        return prop;
    }

    private static boolean changeSkin(final plasmaSwitchboard sb, final String skinPath, final String skin) {
        final File htdocsDir = new File(sb.getConfigPath("htDocsPath", "DATA/HTDOCS"), "env");
        final File styleFile = new File(htdocsDir, "style.css");
        final File skinFile = new File(skinPath, skin);

        styleFile.getParentFile().mkdirs();
        try {
            FileUtils.copy(skinFile, styleFile);
            sb.setConfig("currentSkin", skin.substring(0, skin.length() - 4));
            return true;
        } catch (final IOException e) {
            return false;
        }
    }
}
