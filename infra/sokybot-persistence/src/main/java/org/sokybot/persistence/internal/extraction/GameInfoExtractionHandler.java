package org.sokybot.persistence.internal.extraction;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityManagerFactory;

import org.sokybot.persistence.entities.Division;
import org.sokybot.persistence.entities.DivisionInfo;
import org.sokybot.persistence.entities.GameInfo;
import org.sokybot.pk2extractor.dto.gameinfo.GameInfoData;
import org.sokybot.pk2extractor.mediapk2.gameinfo.GameInfoExtractor;

public class GameInfoExtractionHandler extends BasePk2ExtractionHandler<GameInfoData, GameInfo> {

    private final String gamePath;
    private long currentPk2Modified;
    private long currentPk2Size;

    public void setPk2Metadata(long modified, long size) {
        this.currentPk2Modified = modified;
        this.currentPk2Size = size;
    }

    public GameInfoExtractionHandler(EntityManagerFactory emf, String gamePath) {
        super(emf, new GameInfoExtractor());
        this.gamePath = gamePath;
    }

    @Override
    public String getExtractionType() {
        return "Game Info";
    }

    @Override
    protected GameInfo convertDtoToEntity(GameInfoData dto) {
        if (dto == null) {
            return null;
        }

        GameInfo entity = new GameInfo();
        entity.setGamePath(this.gamePath);
        entity.setPort(dto.getPort());
        entity.setVersion(dto.getVersion());

        if (dto.getDivisionInfo() != null) {
            DivisionInfo divInfo = new DivisionInfo();
            divInfo.local = dto.getDivisionInfo().getLocal();

            if (dto.getDivisionInfo().getDivisions() != null) {
                List<Division> divisionsList = dto.getDivisionInfo().getDivisions().stream()
                        .map(dDto -> {
                            Division div = new Division();
                            div.setName(dDto.getName());
                            if (dDto.getHosts() != null) {
                                for (String h : dDto.getHosts()) {
                                    div.addHost(h);
                                }
                            }
                            return div;
                        })
                        .collect(Collectors.toList());

                for (Division d : divisionsList) {
                    divInfo.addDivision(d);
                }
            }
            entity.setDivisionInfo(divInfo);
        }

        // SilkroadType handling (if implemented in extractor/DTO)
        // Ignoring for now as it's null in current Extractor implementation

        entity.setLastPk2Modified(currentPk2Modified);
        entity.setLastPk2Size(currentPk2Size);

        return entity;
    }
}
