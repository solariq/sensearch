package com.expleague.sensearch.metrics;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class UserAgents {

  private List<String> agentsList = new ArrayList<>();
  private Random randomGenerator = new Random();

  private void initAgents() {
    agentsList.add("Mozilla/5.0 (Windows NT 6.3; WOW64; rv:36.0) Gecko/20100101 Firefox/36.0");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.116 Safari/537.36");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/40.0.2214.115 Safari/537.36");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.101 Safari/537.36 OPR/40.0.2308.62");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; .NET4.0C; .NET4.0E; rv:11.0) like Gecko");
    agentsList.add("Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; ASU2JS; rv:11.0) like Gecko");
    agentsList.add(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2486.0 Safari/537.36 Edge/13.10586");
  }


  public UserAgents() {
    initAgents();
  }


  private String anyUserAgent() {
    int index = randomGenerator.nextInt(agentsList.size());
    return agentsList.get(index);
  }

  public void setAnyAgent(URLConnection connection) {
    connection.setRequestProperty("User-Agent",
        anyUserAgent());
  }

}
