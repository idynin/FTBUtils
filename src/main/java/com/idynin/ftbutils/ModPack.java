/*
 * This file is part of FTB Launcher.
 * 
 * THIS FILE HAS BEEN MODIFIED FOR INCLUSION IN FTBUtils Modifications Copyright © 2014 Ilya Dynin
 * 
 * Copyright © 2012-2013, FTB Launcher Contributors <https://github.com/Slowpoke101/FTBLaunch/> FTB
 * Launcher is licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.idynin.ftbutils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ModPack {
  private String name, author, version, url, dir, mcVersion, serverUrl, logoName, imageName, info,
      animation, maxPermSize, xml, hasBundledMap;
  private String[] mods, oldVersions;
  private int index;
  private boolean updated = false, hasCustomTP;
  private final static ArrayList<ModPack> packs = new ArrayList<ModPack>();
  private boolean privatePack;

  public static void removePacks(String xml) {
    ArrayList<ModPack> remove = new ArrayList<ModPack>();
    int removed = -1;
    for (ModPack pack : packs) {
      if (pack.getParentXml().equalsIgnoreCase(xml)) {
        remove.add(pack);
      }
    }
    for (ModPack pack : remove) {
      removed = pack.getIndex();
      packs.remove(pack);
    }
    for (ModPack pack : packs) {
      if (removed != -1 && pack.getIndex() > removed) {
        pack.setIndex(pack.getIndex() - 1);
      }
    }
  }

  /**
   * Used to get the List of modpacks
   * 
   * @return - the array containing all the modpacks
   */
  public static ArrayList<ModPack> getPackArray() {
    return packs;
  }

  /**
   * Gets the ModPack form the array and the given index
   * 
   * @param i - the value in the array
   * @return - the ModPack based on the i value
   */
  public static ModPack getPack(int i) {
    return packs.get(i);
  }

  public static ModPack getPack(String dir) {
    for (ModPack pack : packs) {
      if (pack.getDir().equalsIgnoreCase(dir))
        return pack;
    }
    return null;
  }

  /**
   * Adds modpack to the modpacks array
   * 
   * @param pack - a ModPack instance
   */
  public static void addPack(ModPack pack) {
    synchronized (packs) {
      packs.add(pack);
    }
  }

  /**
   * Constructor for ModPack class
   * 
   * @param name - the name of the ModPack
   * @param author - the author of the ModPack
   * @param version - the version of the ModPack
   * @param logo - the logo file name for the ModPack
   * @param url - the ModPack file name
   * @param image - the splash image file name for the ModPack
   * @param dir - the directory for the ModPack
   * @param mcVersion - the minecraft version required for the ModPack
   * @param serverUrl - the server file name of the ModPack
   * @param info - the description for the ModPack
   * @param mods - string containing a list of mods included in the ModPack by default
   * @param oldVersions - string containing all available old versions of the ModPack
   * @param animation - the animation to display before minecraft launches
   * @param idx - the actual position of the modpack in the index
   * @param bundledMap - pack has map bundled inside it
   * @param customTP - pack does not use primary TP's for MC version
   * @throws IOException
   * @throws NoSuchAlgorithmException
   */
  public ModPack(String name, String author, String version, String logo, String url, String image,
      String dir, String mcVersion, String serverUrl, String info, String mods, String oldVersions,
      String animation, String maxPermSize, int idx, boolean privatePack, String xml,
      String bundledMap, boolean customTP) throws IOException, NoSuchAlgorithmException {
    index = idx;
    this.name = name;
    this.author = author;
    this.version = version;
    this.dir = dir;
    this.mcVersion = mcVersion;
    this.url = url;
    this.serverUrl = serverUrl;
    this.privatePack = privatePack;
    this.xml = xml;
    this.maxPermSize = maxPermSize;
    this.hasBundledMap = bundledMap;
    this.hasCustomTP = customTP;
    if (!animation.isEmpty()) {
      this.animation = animation;
    } else {
      this.animation = "empty";
    }
    logoName = logo;
    imageName = image;
    this.info = info;
    if (mods.isEmpty()) {
      this.mods = null;
    } else {
      this.mods = mods.split("; ");
    }
    if (oldVersions.isEmpty()) {
      this.oldVersions = null;
    } else {
      this.oldVersions = oldVersions.split(";");
    }

  }

  /**
   * Used to check if the cached items are up to date
   * 
   * @param verFile - the version file to check
   * @return checks the version file against the current modpack version
   */
  @SuppressWarnings("unused")
  private boolean upToDate(File verFile) {
    String storedVersion = getStoredVersion(verFile).replace(".", "");

    if ("".equals(storedVersion)
        || Integer.parseInt(storedVersion) != Integer.parseInt(version.replace(".", ""))) {
      try {
        if (!verFile.exists()) {
          verFile.getParentFile().mkdirs();
          verFile.createNewFile();
        }
        BufferedWriter out = new BufferedWriter(new FileWriter(verFile));
        out.write(version);
        out.flush();
        out.close();
        return false;
      } catch (IOException e) {
        e.printStackTrace();
        return false;
      }
    }

    return true;
  }

  public boolean needsUpdate(File verFile) {
    return Integer.parseInt(getStoredVersion(verFile).replace(".", "")) != Integer.parseInt(version
        .replace(".", ""));
  }

  public String getStoredVersion(File verFile) {
    String result = "";
    try {
      if (!verFile.exists()) {
        verFile.getParentFile().mkdirs();
        verFile.createNewFile();
      }
      BufferedReader in = new BufferedReader(new FileReader(verFile));
      String line;
      if ((line = in.readLine()) != null) {
        result = line;
      }
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return result;
  }

  /**
   * Used to get index of modpack
   * 
   * @return - the index of the modpack in the GUI
   */
  public int getIndex() {
    return index;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  /**
   * Used to get name of modpack
   * 
   * @return - the name of the modpack
   */
  public String getName() {
    return name;
  }

  /**
   * Used to get Author of modpack
   * 
   * @return - the modpack's author
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Used to get the version of the modpack
   * 
   * @return - the modpacks version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Used to get the URL or File name of the modpack
   * 
   * @return - the modpacks URL
   */
  public String getUrl() {
    return url;
  }

  /**
   * Used to get the directory of the modpack
   * 
   * @return - the directory for the modpack
   */
  public String getDir() {
    return dir;
  }

  /**
   * Used to get the minecraft version required for the modpack
   * 
   * @return - the minecraft version
   */
  public String getMcVersion() {
    return mcVersion;
  }

  /**
   * Used to get the info or description of the modpack
   * 
   * @return - the info for the modpack
   */
  public String getInfo() {
    return info;
  }

  /**
   * Used to get an array of mods inside the modpack
   * 
   * @return - string array of all mods contained
   */
  public String[] getMods() {
    return mods;
  }

  /**
   * Used to get the name of the server file for the modpack
   * 
   * @return - string representing server file name
   */
  public String getServerUrl() {
    return serverUrl;
  }

  /**
   * Used to get the logo file name
   * 
   * @return - the logo name as saved on the repo
   */
  public String getLogoName() {
    return logoName;
  }

  /**
   * Used to get the splash file name
   * 
   * @return - the splash image name as saved on the repo
   */
  public String getImageName() {
    return imageName;
  }

  /**
   * Used to set whether the modpack has been updated
   * 
   * @param result - the status of whether the modpack has been updated or not
   */
  public void setUpdated(boolean result) {
    updated = result;
  }

  /**
   * Used to check if the modpack has been updated
   * 
   * @return - the boolean representing whether the modpack has been updated
   */
  public boolean isUpdated() {
    return updated;
  }

  /**
   * Used to get all available old versions of the modpack
   * 
   * @return - string array containing all available old version of the modpack
   */
  public String[] getOldVersions() {
    return oldVersions;
  }

  /**
   * Used to set the minecraft version required of the pack to a custom version
   * 
   * @param version - the version of minecraft for the pack
   */
  public void setMcVersion(String version) {
    mcVersion = version;
  }

  /**
   * @return the filename of the gif animation to display before minecraft loads
   */
  public String getAnimation() {
    return animation;
  }

  public boolean isPrivatePack() {
    return privatePack;
  }

  public String getParentXml() {
    return xml;
  }

  public String getMaxPermSize() {
    return maxPermSize;
  }

  public String getBundledMap() {
    return hasBundledMap;
  }

  public boolean hasCustomTP() {
    return hasCustomTP;
  }

  /**
   * Lightly modified from FTB net.ftb.workers.ModpackLoader
   * 
   * @param modPackFilter
   */
  public static void populateModpacks(String server, String modpackMetaPath,
      ModPackFilter modPackFilter) {
    InputStream modPackStream = null;
    if (modPackStream == null) {
      try {
        modPackStream = new URL("http://" + server + modpackMetaPath).openStream();
      } catch (IOException e) {
        System.err
            .println("Completely unable to download the modpack file - check your connection");
        e.printStackTrace();
      }
    }
    if (modPackStream != null) {
      Document doc;
      try {
        doc = getXML(modPackStream);
      } catch (Exception e) {
        System.err.println("Exception reading modpack file");
        e.printStackTrace();
        return;
      }
      if (doc == null) {
        System.err.println("Error: could not load modpack data!");
        return;
      }
      NodeList modPacks = doc.getElementsByTagName("modpack");
      ModPack mp;
      for (int i = 0; i < modPacks.getLength(); i++) {
        Node modPackNode = modPacks.item(i);
        NamedNodeMap modPackAttr = modPackNode.getAttributes();
        try {
          mp =
              new ModPack(modPackAttr.getNamedItem("name").getTextContent(), modPackAttr
                  .getNamedItem("author").getTextContent(), modPackAttr.getNamedItem("version")
                  .getTextContent(), modPackAttr.getNamedItem("logo").getTextContent(), modPackAttr
                  .getNamedItem("url").getTextContent(), modPackAttr.getNamedItem("image")
                  .getTextContent(), modPackAttr.getNamedItem("dir").getTextContent(), modPackAttr
                  .getNamedItem("mcVersion").getTextContent(), modPackAttr.getNamedItem(
                  "serverPack").getTextContent(), modPackAttr.getNamedItem("description")
                  .getTextContent(), modPackAttr.getNamedItem("mods") != null ? modPackAttr
                  .getNamedItem("mods").getTextContent() : "",
                  modPackAttr.getNamedItem("oldVersions") != null ? modPackAttr.getNamedItem(
                      "oldVersions").getTextContent() : "",
                  modPackAttr.getNamedItem("animation") != null ? modPackAttr.getNamedItem(
                      "animation").getTextContent() : "",
                  modPackAttr.getNamedItem("maxPermSize") != null ? modPackAttr.getNamedItem(
                      "maxPermSize").getTextContent() : "", (ModPack.getPackArray().isEmpty() ? 0
                      : ModPack.getPackArray().size()), false, "modpacks.xml",
                  modPackAttr.getNamedItem("bundledMap") != null ? modPackAttr.getNamedItem(
                      "bundledMap").getTextContent() : "",
                  modPackAttr.getNamedItem("customTP") != null ? true : false);
          if (modPackFilter == null || modPackFilter.accept(mp)) {
            ModPack.addPack(mp);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      IOUtils.closeQuietly(modPackStream);
    }
  }

  /**
   * From net.ftb.util.AppUtils
   * 
   * Reads XML from a stream
   * 
   * @param stream the stream to read the document from
   * @return The document
   * @return The document
   * @throws IOException , SAXException if an error occurs when reading from the stream
   */
  public static Document getXML(InputStream stream) throws IOException, SAXException {
    DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
    try {
      return docFactory.newDocumentBuilder().parse(stream);
    } catch (ParserConfigurationException ignored) {
    } catch (UnknownHostException e) {
    }
    return null;
  }
}
