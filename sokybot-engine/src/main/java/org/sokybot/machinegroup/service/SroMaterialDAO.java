package org.sokybot.machinegroup.service;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import org.sokybot.app.AppConstants;
import org.sokybot.persistence.entities.DivisionInfo;
import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.persistence.entities.LvlEXP;
import org.sokybot.persistence.entities.MasteryData;
import org.sokybot.persistence.entities.SilkroadEntity;
import org.sokybot.persistence.entities.SilkroadType;
import org.sokybot.persistence.entities.geo.Vector2D;
import org.sokybot.persistence.entities.ItemEntity;
import org.sokybot.persistence.entities.ObjectNavMesh;
import org.sokybot.persistence.entities.navmesh.Position;
import org.sokybot.persistence.entities.SectorRef;
import org.sokybot.persistence.entities.NPCEntity;
import org.sokybot.persistence.entities.ShopEntity;
import org.sokybot.persistence.entities.PortalEntity;
import org.sokybot.persistence.entities.TeleportEntity;
import org.sokybot.persistence.entities.SkillEntity;
import org.sokybot.machinegroup.mapnavigation.RuteFinder;
import org.sokybot.machinegroup.mapnavigation.Sector;
// import org.sokybot.builders.extractor.IEntityExtractorFactory;
// import org.sokybot.builders.extractor.IPK2File;
import org.sokybot.persistence.service.GameInfoRepository;
import org.sokybot.persistence.service.ItemEntityRepository;
import org.sokybot.persistence.service.LvlEXPRepository;
import org.sokybot.persistence.service.MasteryDataRepository;
import org.sokybot.persistence.service.NPCEntityRepository;
import org.sokybot.persistence.service.ObjectNavMeshRepository;
import org.sokybot.persistence.service.PortalEntityRepository;
import org.sokybot.persistence.service.SectorRefRepository;
import org.sokybot.persistence.service.ShopEntityRepository;
import org.sokybot.persistence.service.SkillEntityRepository;
import org.sokybot.persistence.service.TeleportEntityRepository;
import org.sokybot.pk2.IPk2Driver;
import org.sokybot.utils.DDSReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

@Deprecated
public class SroMaterialDAO {
    // Legacy class - replaced by IGameDataLookup, IMediaAssetProvider, IRuteFinder
}
