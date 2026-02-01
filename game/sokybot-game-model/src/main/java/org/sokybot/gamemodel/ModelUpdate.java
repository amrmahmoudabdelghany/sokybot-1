package org.sokybot.gamemodel;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ModelUpdate<T> {
    private T entity;
    private ModelUpdateType type;
}
