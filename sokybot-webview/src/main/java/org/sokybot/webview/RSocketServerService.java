package org.sokybot.webview;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.WebsocketServerTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import io.rsocket.RSocket;
import io.rsocket.Payload;
import io.rsocket.ConnectionSetupPayload;

import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.gamemodel.model.ITrainer;
import java.util.function.Function;
import java.util.concurrent.ConcurrentHashMap;

@Component(immediate = true)
public class RSocketServerService {
    private static final Logger logger = LoggerFactory.getLogger(RSocketServerService.class);
    private CloseableChannel server;
    private final ObjectMapper mapper = new ObjectMapper();

    private GameEventBridge eventBridge;
    private IGroupContext groupContext; // Keep for backward compat or default?
    private org.sokybot.runtime.ISokybotContext sokybotContext;
    
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL)
    private WebviewConfigurator extensionConfigurator;
    
    // Stream handlers registry
    private final Map<String, Function<Map<String, Object>, Flux<Map<String, Object>>>> streamHandlers = 
        new ConcurrentHashMap<>();
    
    // Event sink for extension events
    private final reactor.core.publisher.Sinks.Many<Map<String, Object>> extensionEventSink = 
        reactor.core.publisher.Sinks.many().multicast().onBackpressureBuffer(100);

    @Reference
    protected void setEventBridge(GameEventBridge eventBridge) {
        this.eventBridge = eventBridge;
    }
    
    @Reference(cardinality = org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL)
    protected void setGroupContext(IGroupContext groupContext) {
        this.groupContext = groupContext;
    }

    @Reference
    protected void setSokybotContext(org.sokybot.runtime.ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
    }
    
    public void registerExtensionConfigurator(WebviewConfigurator configurator) {
        // This method is called by WebviewConfigurator when it's activated
        // The @Reference above will also set it, so this is just for explicit registration
    }
    
    public void registerStreamHandler(String streamKey, Function<Map<String, Object>, Flux<Map<String, Object>>> handler) {
        streamHandlers.put(streamKey, handler);
        logger.debug("Registered stream handler: {}", streamKey);
    }
    
    public void sendExtensionEvent(String eventType, Map<String, Object> data) {
        Map<String, Object> event = new HashMap<>();
        event.put("type", eventType);
        event.put("data", data);
        event.put("timestamp", System.currentTimeMillis());
        extensionEventSink.tryEmitNext(event);
    }

    @Activate
    public void start() {
        logger.info("Starting RSocket Server on port 7000");
        this.server = RSocketServer.create(new SocketAcceptor() {
            @Override
            public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
                return Mono.just(new RSocket() {
                    @Override
                    public Mono<Payload> requestResponse(Payload payload) {
                         String requestData = payload.getDataUtf8();
                         logger.info("Received request: " + requestData);
                         
                         try {
                             if (requestData.startsWith("getCharacterState")) {
                                 // Simple parsing for now: "getCharacterState:machineId" or just "getCharacterState" (use first machine)
                                 String machineId = null;
                                 if (requestData.contains(":")) {
                                     machineId = requestData.substring(requestData.indexOf(":") + 1);
                                 } else {
                                      // Fallback: get first available machine
                                      if (groupContext != null && groupContext.getMachines().length > 0) {
                                          machineId = groupContext.getMachines()[0].name(); // or fullName?
                                          // groupContext.getMachines() are IMachineContext objects
                                          // check findMachineCtx expects just name?
                                      }
                                 }
                                 
                                 ITrainer trainer = null;
                                 IMachineContext ctx = null;
                                 if (machineId != null && groupContext != null) {
                                      String simpleName = machineId.contains(".") 
                                          ? machineId.substring(machineId.lastIndexOf(".") + 1) 
                                          : machineId;
                                          
                                      ctx = groupContext.findMachineCtx(simpleName).orElse(null);
                                      if (ctx != null && ctx.getGameModel() != null) {
                                          trainer = ctx.getGameModel().getTrainer();
                                      }
                                 }
                                 
                                 if (trainer != null) {
                                     Map<String, Object> state = new HashMap<>();
                                     state.put("entityId", trainer.getUniqueId());
                                     state.put("characterName", trainer.getName());
                                     state.put("level", trainer.getLevel());
                                     state.put("currentHP", trainer.getCurrentHP());
                                     state.put("maxHP", trainer.getMaxHP());
                                     state.put("currentMP", trainer.getCurrentMP());
                                     state.put("maxMP", trainer.getMaxMP());
                                     state.put("gold", trainer.getGold());
                                     state.put("xSector", trainer.getRefId());
                                     // Add more fields as needed for UI
                                     state.put("x", trainer.getX());
                                     state.put("y", trainer.getY());
                                     
                                     // Engine Status
                                     state.put("isRunning", ctx != null && ctx.isRunning());
                                     
                                     return Mono.just(DefaultPayload.create(mapper.writeValueAsString(state)));
                                 }
                                 return Mono.just(DefaultPayload.create("{}"));
                             } else if (requestData.startsWith("startBot")) {
                                 String machineId = requestData.contains(":") ? requestData.split(":")[1] : null;
                                 if (machineId != null) {
                                     IMachineContext ctx = groupContext.findMachineCtx(machineId).orElse(null);
                                     if (ctx != null) {
                                         ctx.getEngine().start();
                                         return Mono.just(DefaultPayload.create("{\"status\":\"started\"}")); 
                                     }
                                 }
                                 return Mono.just(DefaultPayload.create("{\"status\":\"error\"}"));
                             } else if (requestData.startsWith("stopBot")) {
                                 String machineId = requestData.contains(":") ? requestData.split(":")[1] : null;
                                  if (machineId != null && groupContext != null) {
                                     IMachineContext ctx = groupContext.findMachineCtx(machineId).orElse(null);
                                     if (ctx != null) {
                                         ctx.getEngine().stop();
                                         return Mono.just(DefaultPayload.create("{\"status\":\"stopped\"}")); 
                                     }
                                 }
                                 return Mono.just(DefaultPayload.create("{\"status\":\"error\"}"));
                             } else if ("getMachines".equals(requestData)) {
                                 if (groupContext != null) {
                                     IMachineContext[] machines = groupContext.getMachines();
                                     List<String> machineNames = new ArrayList<>();
                                     for(IMachineContext m : machines) {
                                         // Use fullName to match page registration format
                                         machineNames.add(m.fullName());
                                     }
                                     return Mono.just(DefaultPayload.create(mapper.writeValueAsString(machineNames)));
                                 }
                                 return Mono.just(DefaultPayload.create("[]"));
                             }
                              
                              //
                              // Management Handlers
                              //
                              
                              else if (requestData.startsWith("createGroup")) {
                                   try {
                                       Map<String, String> params = mapper.readValue(requestData.substring(requestData.indexOf(":")+1), Map.class);
                                       String name = params.get("name");
                                       String path = params.get("path");
                                       if (name != null && path != null && sokybotContext != null) {
                                           sokybotContext.installGroup(name, path);
                                           return Mono.just(DefaultPayload.create("{\"status\":\"success\"}"));
                                       }
                                   } catch(Exception e) {
                                       logger.error("Create Group Failed", e);
                                   }
                                   return Mono.just(DefaultPayload.create("{\"status\":\"error\"}"));
                                   
                              } else if (requestData.startsWith("createMachine")) {
                                   try {
                                       Map<String, Object> params = mapper.readValue(requestData.substring(requestData.indexOf(":")+1), Map.class);
                                       String group = (String) params.get("group");
                                       String name = (String) params.get("name");
                                       List<String> options = (List<String>) params.get("options");
                                       
                                       if (group != null && name != null && sokybotContext != null) {
                                           IGroupContext grpCtx = sokybotContext.findGroupCtx(group).orElse(null);
                                           if (grpCtx != null) {
                                               grpCtx.installMachine(name, options.toArray(new String[0]));
                                                return Mono.just(DefaultPayload.create("{\"status\":\"success\"}"));
                                           }
                                       }
                                   } catch(Exception e) {
                                       logger.error("Create Machine Failed", e);
                                   }
                                    return Mono.just(DefaultPayload.create("{\"status\":\"error\"}"));
                                    
                              } else if ("getGroups".equals(requestData)) {
                                  if (sokybotContext != null) {
                                      String[] names = sokybotContext.listNames();
                                      return Mono.just(DefaultPayload.create(mapper.writeValueAsString(names)));
                                  }
                                  return Mono.just(DefaultPayload.create("[]"));
                                  
                              } else if (requestData.startsWith("getGroupDetails")) {
                                  String groupName = requestData.contains(":") ? requestData.split(":")[1] : null;
                                  if (groupName != null && sokybotContext != null) {
                                       IGroupContext grpCtx = sokybotContext.findGroupCtx(groupName).orElse(null);
                                       if (grpCtx != null) {
                                           org.sokybot.service.ISroDAO gameDao = grpCtx.getGameDAO();
                                           Map<String, Object> details = new HashMap<>();
                                           details.put("version", gameDao.getVersion());
                                           details.put("hosts", gameDao.getDivHosts());
                                           return Mono.just(DefaultPayload.create(mapper.writeValueAsString(details)));
                                       }
                                  }
                                  return Mono.just(DefaultPayload.create("{}"));
                              } else if ("getMachines".equals(requestData) && sokybotContext != null) {
                                  // Global machines list? or per groups?
                                  // Legacy behavior: list all machines or grouped?
                                  // Let's iterate all groups and collect machines
                                  List<String> allMachines = new ArrayList<>();
                                  for (IGroupContext g : sokybotContext.getGroups()) {
                                      for (IMachineContext m : g.getMachines()) {
                                          allMachines.add(m.fullName()); 
                                          // fullName includes group prefix usually?
                                      }
                                  }
                                  return Mono.just(DefaultPayload.create(mapper.writeValueAsString(allMachines)));
                              } else if (requestData.startsWith("fs.list")) {
                                   String path = requestData.contains(":") ? requestData.substring(requestData.indexOf(":") + 1) : ".";
                                   if (path == null || path.trim().isEmpty()) path = ".";
                                   
                                   java.io.File dir = new java.io.File(path);
                                   if (!dir.exists()) {
                                       // Try parsing assuming it might be a drive letter or root if '.' fails or standardizes
                                   }
                                   
                                   List<Map<String, Object>> files = new ArrayList<>();
                                   // Add parent ref if not root
                                   if (dir.getParentFile() != null) {
                                       Map<String, Object> p = new HashMap<>();
                                       p.put("name", "..");
                                       p.put("path", dir.getParentFile().getAbsolutePath());
                                       p.put("isDirectory", true);
                                       files.add(p);
                                   }
                                   
                                   if (dir.exists() && dir.isDirectory()) {
                                       java.io.File[] children = dir.listFiles();
                                       if (children != null) {
                                           for (java.io.File f : children) {
                                                if (f.isHidden()) continue;
                                                Map<String, Object> fInfo = new HashMap<>();
                                                fInfo.put("name", f.getName());
                                                fInfo.put("path", f.getAbsolutePath());
                                                fInfo.put("isDirectory", f.isDirectory());
                                                files.add(fInfo);
                                           }
                                       }
                                   } else {
                                       // Maybe it's a drive list request if path is special? 
                                       // For now start at "."
                                   }
                                   
                                   // sorting: directories first
                                   files.sort((a, b) -> {
                                       boolean aDir = (Boolean) a.get("isDirectory");
                                       boolean bDir = (Boolean) b.get("isDirectory");
                                       if (aDir && !bDir) return -1;
                                       if (!aDir && bDir) return 1;
                                       return ((String) a.get("name")).compareToIgnoreCase((String) b.get("name"));
                                   });
                                   
                                   Map<String, Object> result = new HashMap<>();
                                   result.put("current", dir.getAbsolutePath());
                                   result.put("files", files);
                                   
                                   return Mono.just(DefaultPayload.create(mapper.writeValueAsString(result)));
                                   
                               } else if ("fs.roots".equals(requestData)) {
                                   java.io.File[] roots = java.io.File.listRoots();
                                   List<Map<String, Object>> rootList = new ArrayList<>();
                                   if (roots != null) {
                                       for (java.io.File f : roots) {
                                           Map<String, Object> r = new HashMap<>();
                                           r.put("name", f.getPath());
                                           r.put("path", f.getPath());
                                           r.put("isDirectory", true);
                                           rootList.add(r);
                                       }
                                   }
                                   return Mono.just(DefaultPayload.create(mapper.writeValueAsString(rootList)));
                               }
                               
                               //
                               // Extension Handlers
                               //
                               
                               else if (requestData.startsWith("extension.schema:")) {
                                   String[] parts = requestData.split(":", 3);
                                   String pageId = parts.length > 1 ? parts[1] : null;
                                   String machineId = parts.length > 2 ? parts[2] : null;
                                   
                                   if (pageId != null && extensionConfigurator != null) {
                                       String request = machineId != null ? machineId : "";
                                       Map<String, Object> response = extensionConfigurator.handleSchemaRequest(pageId, request);
                                       return Mono.just(DefaultPayload.create(mapper.writeValueAsString(response)));
                                   }
                                   return Mono.just(DefaultPayload.create("{}"));
                                   
                               } else if (requestData.startsWith("extension.action:")) {
                                   String[] parts = requestData.split(":", 3);
                                   String pageId = parts.length > 1 ? parts[1] : null;
                                   String actionData = parts.length > 2 ? parts[2] : null;
                                   
                                   if (pageId != null && extensionConfigurator != null) {
                                       try {
                                           // Parse action and data from actionData
                                           // Format: "actionName:jsonData"
                                           String action = null;
                                           Map<String, Object> data = new HashMap<>();
                                           
                                           if (actionData != null && actionData.contains(":")) {
                                               int colonIndex = actionData.indexOf(":");
                                               action = actionData.substring(0, colonIndex);
                                               String jsonData = actionData.substring(colonIndex + 1);
                                               if (!jsonData.isEmpty()) {
                                                   data = mapper.readValue(jsonData, Map.class);
                                               }
                                           } else if (actionData != null) {
                                               action = actionData;
                                           }
                                           
                                           if (action != null) {
                                               Map<String, Object> response = extensionConfigurator.handleAction(pageId, action, data);
                                               return Mono.just(DefaultPayload.create(mapper.writeValueAsString(response)));
                                           }
                                       } catch (Exception e) {
                                           logger.error("Error handling extension action", e);
                                           return Mono.just(DefaultPayload.create("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}"));
                                       }
                                   }
                                   return Mono.just(DefaultPayload.create("{\"success\":false,\"error\":\"No handler found\"}"));
                                   
                               } else if ("extension.registry".equals(requestData)) {
                                   if (extensionConfigurator != null) {
                                       Map<String, Object> registry = extensionConfigurator.getExtensionRegistry();
                                       return Mono.just(DefaultPayload.create(mapper.writeValueAsString(registry)));
                                   }
                                   return Mono.just(DefaultPayload.create("{}"));
                                   
                               } else if (requestData.startsWith("extension.toolbar.action:")) {
                                   // Format: extension.toolbar.action:<actionId>:<action>:<jsonData>
                                   String[] parts = requestData.split(":", 4);
                                   String actionId = parts.length > 1 ? parts[1] : null;
                                   String action = parts.length > 2 ? parts[2] : null;
                                   String jsonData = parts.length > 3 ? parts[3] : "{}";
                                   
                                   if (actionId != null && action != null && extensionConfigurator != null) {
                                       try {
                                           Map<String, Object> data = mapper.readValue(jsonData, Map.class);
                                           Map<String, Object> response = extensionConfigurator.handleToolbarAction(actionId, action, data);
                                           return Mono.just(DefaultPayload.create(mapper.writeValueAsString(response)));
                                       } catch (Exception e) {
                                           logger.error("Error handling toolbar action", e);
                                           return Mono.just(DefaultPayload.create("{\"success\":false,\"error\":\"" + e.getMessage() + "\"}"));
                                       }
                                   }
                                   return Mono.just(DefaultPayload.create("{\"success\":false,\"error\":\"Invalid toolbar action request\"}"));
                               }
                         } catch (Exception e) {
                             logger.error("Error processing request", e);
                             return Mono.error(e);
                         }
                         
                         return Mono.just(DefaultPayload.create("Echo: " + requestData));
                    }

                    @Override
                    public Flux<Payload> requestStream(Payload payload) {
                        String requestData = payload.getDataUtf8();
                        logger.debug("Received stream request: " + requestData);
                        
                        try {
                            // Handle extension streams
                            if (requestData.startsWith("extension.stream:")) {
                                String[] parts = requestData.split(":", 4);
                                String pageId = parts.length > 1 ? parts[1] : null;
                                String streamId = parts.length > 2 ? parts[2] : null;
                                String paramsJson = parts.length > 3 ? parts[3] : "{}";
                                
                                if (pageId != null && streamId != null) {
                                    String streamKey = pageId + ":" + streamId;
                                    Function<Map<String, Object>, Flux<Map<String, Object>>> handler = 
                                        streamHandlers.get(streamKey);
                                    
                                    if (handler != null) {
                                        Map<String, Object> params = mapper.readValue(paramsJson, Map.class);
                                        Flux<Map<String, Object>> dataStream = handler.apply(params);
                                        
                                        return dataStream
                                            .map(data -> {
                                                try {
                                                    String json = mapper.writeValueAsString(data);
                                                    return DefaultPayload.create(json);
                                                } catch (Exception e) {
                                                    logger.error("Error serializing stream data", e);
                                                    return DefaultPayload.create("{\"error\":\"serialization_failed\"}");
                                                }
                                            })
                                            .onErrorResume(error -> {
                                                logger.error("Stream error for " + streamKey, error);
                                                return Flux.just(DefaultPayload.create(
                                                    "{\"error\":\"" + error.getMessage() + "\"}"
                                                ));
                                            });
                                    } else {
                                        logger.warn("Stream handler not found: " + streamKey);
                                    }
                                }
                                
                                return Flux.just(DefaultPayload.create("{\"error\":\"stream_not_found\"}"));
                            }
                            
                            // Handle extension events stream
                            if (requestData.equals("extension.events")) {
                                return extensionEventSink.asFlux()
                                    .map(event -> {
                                        try {
                                            return DefaultPayload.create(mapper.writeValueAsString(event));
                                        } catch (Exception e) {
                                            logger.error("Error serializing extension event", e);
                                            return DefaultPayload.create("{\"error\":\"serialization_failed\"}");
                                        }
                                    });
                            }
                            
                            // Default: game event stream
                            return eventBridge.getEventStream()
                                .map(event -> {
                                    try {
                                        return DefaultPayload.create(mapper.writeValueAsString(event));
                                    } catch (JsonProcessingException e) {
                                        logger.error("Error serializing event", e);
                                        return DefaultPayload.create("{\"error\": \"serialization_failed\"}");
                                    }
                                });
                        } catch (Exception e) {
                            logger.error("Error processing stream request", e);
                            return Flux.error(e);
                        }
                    }
                });
            }
        })
        .bindNow(WebsocketServerTransport.create(7000));
        
        logger.info("RSocket Server started on port 7000");
    }

    @Deactivate
    public void stop() {
        if (this.server != null) {
            this.server.dispose();
            logger.info("RSocket Server stopped");
        }
    }
}
