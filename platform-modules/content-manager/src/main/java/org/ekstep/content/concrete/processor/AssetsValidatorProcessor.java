package org.ekstep.content.concrete.processor;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.ekstep.content.common.AssetsMimeTypeMap;
import org.ekstep.content.common.ContentErrorMessageConstants;
import org.ekstep.content.entity.Manifest;
import org.ekstep.content.entity.Media;
import org.ekstep.content.entity.Plugin;
import org.ekstep.content.enums.ContentErrorCodeConstants;
import org.ekstep.content.enums.ContentWorkflowPipelineParams;
import org.ekstep.content.processor.AbstractProcessor;

import com.ilimi.common.Platform;
import com.ilimi.common.exception.ClientException;
import com.ilimi.common.exception.ServerException;
import com.ilimi.common.logger.PlatformLogger;

public class AssetsValidatorProcessor extends AbstractProcessor {

	public AssetsValidatorProcessor(String basePath, String contentId) {
		if (!isValidBasePath(basePath))
			throw new ClientException(ContentErrorCodeConstants.INVALID_PARAMETER.name(),
					ContentErrorMessageConstants.INVALID_CWP_CONST_PARAM + " | [Path does not Exist.]");
		if (StringUtils.isBlank(contentId))
			throw new ClientException(ContentErrorCodeConstants.INVALID_PARAMETER.name(),
					ContentErrorMessageConstants.INVALID_CWP_CONST_PARAM + " | [Invalid Content Id.]");
		this.basePath = basePath;
		this.contentId = contentId;
	}

	@Override
	protected Plugin process(Plugin plugin) {
		try {
			if (null != plugin) {
				validateAssets(plugin);
			}
		} catch(ClientException ce) {
			throw ce;
		} catch(Exception e) {
			throw new ServerException(ContentErrorCodeConstants.PROCESSOR_ERROR.name(), 
					ContentErrorMessageConstants.PROCESSOR_ERROR + " | [AssetsValidatorProcessor]", e);
		}
		return plugin;
	}
	
	private void validateAssets(Plugin plugin){
		if (null != plugin) {
			Manifest manifest = plugin.getManifest();
			if (null != manifest) {
				List<Media> medias = manifest.getMedias();
				for (Media media: medias) {
					PlatformLogger.log("Validating Asset Id: " + media.getId());
					validateAsset(media);
				}
			}
		}
	}

	private boolean validateAsset(Media media) {
		boolean isValid = false;
		try {
			if (null != media) {
				File file = new File(getAssetPath(media.getType(), media.getSrc()));
				if (file.exists()) {
					if (!isValidAssetMimeType(file))
						throw new ClientException(ContentErrorCodeConstants.INVALID_MIME_TYPE.name(), 
								ContentErrorMessageConstants.INVALID_ASSET_MIMETYPE + " | [Asset " + file.getName() + " has Invalid Mime-Type.]");
					if (!isValidAssetSize(file))
						throw new ClientException(ContentErrorCodeConstants.FILE_SIZE_EXCEEDS_LIMIT.name(), 
								ContentErrorMessageConstants.ASSET_FILE_SIZE_LIMIT_EXCEEDS + " | [Asset " + file.getName() + " is Bigger in Size.]");
					isValid = true;
					PlatformLogger.log("Asset Id '" + media.getId() + "' is Valid.");
				}
			}
		} catch(IOException e) {
			throw new ServerException(ContentErrorCodeConstants.ASSET_FILE_READ.name(), 
					ContentErrorMessageConstants.ASSET_FILE_READ_ERROR, e);
		}
		return isValid;
	}

	private boolean isValidAssetMimeType(File file) throws IOException {
		boolean isValidMimeType = false;
		if (file.exists()) {
			PlatformLogger.log("Validating Asset File '" + file.getName() + "' for Mime-Type.");
			Tika tika = new Tika();
			String mimeType = tika.detect(file);
			isValidMimeType = AssetsMimeTypeMap.isAllowedMimeType(mimeType);
		}
		return isValidMimeType;
	}

	private boolean isValidAssetSize(File file) {
		boolean isValidSize = false;
		if (file.exists()) {
			PlatformLogger.log("Validating Asset File '" + file.getName() + "' for Size.");
			if (file.length() < getAssetFileSizeLimit())
				isValidSize = true;
		}
		return isValidSize;
	}
	
	private double getAssetFileSizeLimit() {
		double size = 20971520;	
		if(Platform.config.hasPath(ContentWorkflowPipelineParams.MAX_ASSET_FILE_SIZE_LIMIT.name()))
			size = Platform.config.getDouble(ContentWorkflowPipelineParams.MAX_ASSET_FILE_SIZE_LIMIT.name());
		return size;
	}

	private String getAssetPath(String type, String src) {
		String path = "";
		if (!StringUtils.isBlank(type) && !StringUtils.isBlank(src)) {
			PlatformLogger.log("Fetching Asset Path.");
			if (isWidgetTypeAsset(type))
				path = basePath + File.separator + ContentWorkflowPipelineParams.widgets.name() + File.separator + src;
			else
				path = basePath + File.separator + ContentWorkflowPipelineParams.assets.name() + File.separator + src;
		}
		return path;
	}
}
