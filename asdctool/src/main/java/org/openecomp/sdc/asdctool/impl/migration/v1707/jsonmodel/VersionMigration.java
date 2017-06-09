package org.openecomp.sdc.asdctool.impl.migration.v1707.jsonmodel;

import fj.data.Either;
import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.openecomp.sdc.asdctool.impl.migration.MigrationException;
import org.openecomp.sdc.asdctool.impl.migration.MigrationMsg;
import org.openecomp.sdc.asdctool.impl.migration.v1707.MigrationUtils;
import org.openecomp.sdc.be.dao.jsongraph.GraphVertex;
import org.openecomp.sdc.be.dao.jsongraph.TitanDao;
import org.openecomp.sdc.be.dao.jsongraph.types.EdgeLabelEnum;
import org.openecomp.sdc.be.dao.titan.TitanGenericDao;
import org.openecomp.sdc.be.dao.titan.TitanOperationStatus;
import org.openecomp.sdc.be.datatypes.enums.NodeTypeEnum;
import org.openecomp.sdc.be.model.Component;
import org.openecomp.sdc.be.model.operations.api.StorageOperationStatus;
import org.openecomp.sdc.be.model.operations.impl.UniqueIdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class VersionMigration<T extends Component>  {

    private static Logger LOGGER = LoggerFactory.getLogger(VersionMigration.class);

    @Resource(name = "titan-generic-dao-migration")
    private TitanGenericDao titanGenericDaoMigration;

    @Resource(name = "titan-dao")
    private TitanDao titanDao;

    public boolean buildComponentsVersionChain(List<T> components) {
        setMissingInvariantUid(components);
        Map<String, List<T>> componentsByInvariant = components.stream().collect(Collectors.groupingBy(Component::getInvariantUUID));
        for (List<T> componentsList : componentsByInvariant.values()) {
            boolean versionChainBuilt = buildVersionChainForInvariant(componentsList);
            if (!versionChainBuilt) {
                titanDao.rollback();
                return false;
            }
            titanDao.commit();
        }
        return true;
    }

    private boolean buildVersionChainForInvariant(List<T> components) {
        sortComponentsByVersion(components);
        for (int i = 0; i < components.size() -1; i++) {
            String lowerVersionUid = components.get(i).getUniqueId();
            String higherVersionUid = components.get(i + 1).getUniqueId();
            boolean versionCreated = createVersionRelationIfNotExist(lowerVersionUid, higherVersionUid);
            if (!versionCreated) {
                return false;
            }
        }
        return true;
    }

    private void sortComponentsByVersion(List<T> components) {
        Collections.sort(components, (o1, o2) -> o1.getVersion().compareTo(o2.getVersion()));
    }

    private boolean createVersionRelationIfNotExist(String fromUid, String toUid) {
        Either<Boolean, TitanOperationStatus> isVersionExists = isVersionExists(fromUid, toUid);
        return isVersionExists.either(versionExists -> versionExists || createVersionRelation(fromUid, toUid),
                               errorStatus -> MigrationUtils.handleError(MigrationMsg.FAILED_TO_RETRIEVE_VERSION_RELATION.getMessage(fromUid, toUid, isVersionExists.right().value().name())));
    }

    private boolean createVersionRelation(String fromUid, String toUid) {
        LOGGER.debug(String.format("creating version edge between vertex %s and vertex %s", fromUid, toUid));
        Either<GraphVertex, TitanOperationStatus> vertexById = titanDao.getVertexById(fromUid);
        Either<GraphVertex, TitanOperationStatus> vertexById1 = titanDao.getVertexById(toUid);
        if (vertexById1.isLeft() && vertexById.isLeft()) {
            TitanOperationStatus versionCreated = titanDao.createEdge(vertexById.left().value(), vertexById1.left().value(), EdgeLabelEnum.VERSION, new HashMap<>());
            return versionCreated == TitanOperationStatus.OK;
        }
        return MigrationUtils.handleError(String.format("could not create version edge between vertex %s and vertex %s.", fromUid, toUid));
    }

    private Either<Boolean, TitanOperationStatus> isVersionExists(String fromUid, String toUid) {
        LOGGER.debug(String.format("checking if version edge between vertex %s and vertex %s already exist", fromUid, toUid));
        String uidKey = UniqueIdBuilder.getKeyByNodeType(getNodeTypeEnum());
        Either<Edge, TitanOperationStatus> edgeByVertices = titanGenericDaoMigration.getEdgeByVerticies(uidKey, fromUid, uidKey, toUid, EdgeLabelEnum.VERSION.name());
        if (isNotFoundStatus(edgeByVertices)) {
            return Either.left(false);
        }
        return edgeByVertices.bimap(foundEdge -> true,
                                    error -> error);
    }

    private boolean isNotFoundStatus(Either<Edge, TitanOperationStatus> edgeByVertices) {
        return edgeByVertices.isRight() && edgeByVertices.right().value() == TitanOperationStatus.NOT_FOUND;
    }

    abstract NodeTypeEnum getNodeTypeEnum();

    //some invariatn uids are missing in production
    private void setMissingInvariantUid(List<T> components) {
        List<T> missingInvariantCmpts = getComponentsWithNoInvariantUids(components);
        for (T missingInvariantCmpt : missingInvariantCmpts) {
            String uuid = missingInvariantCmpt.getUUID();
            missingInvariantCmpt.setInvariantUUID(findInvariantUidOrElseFail(components, uuid));
        }
    }

    private List<T> getComponentsWithNoInvariantUids(List<T> components) {
        List<T> cmptsWithoutInvariant = components.stream().filter(c -> c.getInvariantUUID() == null).collect(Collectors.toList());
        LOGGER.info(String.format("the following components are missing invariant uids: %s", StringUtils.join(cmptsWithoutInvariant.stream().map(Component::getUniqueId).collect(Collectors.toList()), ",")));
        return cmptsWithoutInvariant;
    }

    private String findInvariantUidOrElseFail(List<T> components, String uuid) {
        return components.stream()
                .filter(c -> c.getUUID().equals(uuid))
                .map(Component::getInvariantUUID)
                .filter(c -> c != null)
                .findAny().orElseThrow(() -> new MigrationException(String.format("cannot find invariantuid for component with uuid %s", uuid)));
    }

}