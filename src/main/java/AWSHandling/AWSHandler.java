package AWSHandling;

import bot.Bot;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.waiters.DynamoDbWaiter;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.waiters.S3Waiter;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.FileUpload;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

// All AWS service initialization, as well as service operations, are stored here.
public class AWSHandler {

    private final DynamoDbClient dynamoDbClient;
    private final S3Client s3Client;
    private final S3AsyncClient s3AsyncClient;
    private final Region region;

    // The name that is printed in the logger.
    private static String getLogType() {
        return "AWS";
    }

    // Initializes the clients needed to use AWS services.
    public AWSHandler() {
        this.dynamoDbClient = DynamoDbClient.create();

        this.region = Region.US_EAST_2;
        this.s3Client = S3Client.builder()
                .region(region)
                .build();

        this.s3AsyncClient = S3AsyncClient.crtBuilder()
                .region(Region.US_EAST_2)
                .targetThroughputInGbps(20.0)
                .minimumPartSizeInBytes(8 * MB)
                .build();

        initDynamoDB();
    }

    public void awsClose() {
        dynamoDbClient.close();
        s3Client.close();
        s3AsyncClient.close();

        Bot.log(getLogType(), "Closed all AWS Clients");
    }

	// MANUAL SETUP REQUIRED. Go to the AWS console and input the token under the secret's name ("DiscordRingtones" by default)
    // Retrieves the Discord API Token from AWS Secrets Manager.
    // Modified from example at https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_secrets-manager_code_examples.html
    // > Get a token
    private String retrieveToken(String secret) {
        String key;
        GetSecretValueResponse valueResponse;
        try (SecretsManagerClient smc = SecretsManagerClient.builder().region(Region.US_EAST_1).build()) {
            GetSecretValueRequest valueRequest = GetSecretValueRequest.builder()
                    .secretId(secret)
                    .build();
            valueResponse = smc.getSecretValue(valueRequest);
        } catch (software.amazon.awssdk.services.secretsmanager.model.ResourceNotFoundException e) {
            Bot.log(getLogType(), "API Token not present in secrets manager! Be sure token is present under " + secret + "!");
            return null;
        }

        String returnedString = valueResponse.secretString();

        key = StringUtils.substringsBetween(returnedString, "\"", "\"")[1];

        return key;
    }

    // Calls the private method to get the Discord API Token.
    public String getToken(String secret) {
        return retrieveToken(secret);
    }



    // **************************************************************************************************************
    //
    //                                                   S3 Stuff
    //
    // **************************************************************************************************************
    
    // Most operations were modified from examples in https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_s3_code_examples.html

    // Initializes a S3 Bucket.
    public void initS3Bucket(String bucketName) {
        ListBucketsRequest.builder().build();

        createBucket(bucketName);
    }

	// Bucket names must be DNS compliant. This function ensures prospective bucket names follow this format.
    private String fixBucketName(String name) {
        return name.toLowerCase().replaceAll(" ", "");
    }

    // Creates S3 Buckets if they don't exist.
    private void createBucket(String bucketName) {
    	String fixedBucketName = fixBucketName(bucketName);

        S3Waiter s3Waiter = s3Client.waiter();
        try {
            CreateBucketRequest request = CreateBucketRequest.builder()
                    .bucket(fixedBucketName)
                    .objectOwnership(ObjectOwnership.OBJECT_WRITER)
                    .build();

            PutPublicAccessBlockRequest putPublicAccessBlockRequest = PutPublicAccessBlockRequest.builder()
                    .bucket(fixedBucketName)
                    .publicAccessBlockConfiguration(p -> p.blockPublicAcls(false))
                    .build();

            s3Client.createBucket(request);
            s3Client.putPublicAccessBlock(putPublicAccessBlockRequest);

            HeadBucketRequest bucketRequestWait = HeadBucketRequest.builder()
                    .bucket(fixedBucketName)
                    .build();

            s3Waiter.waitUntilBucketExists(bucketRequestWait);
        } catch (BucketAlreadyOwnedByYouException e) {
            return;
        }
        Bot.log(getLogType(), fixedBucketName + " bucket was created");
    }
    // Determines if an object exists in a bucket.
    private boolean verifyBucketObject(String bucketName, String objectName) {
        String fixedBucketName = fixBucketName(bucketName);

        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(fixedBucketName)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        for (S3Object s3Object : response.contents())
        {
            if (s3Object.key().contains(objectName))
            {
                return true;
            }
        }

        Bot.log(getLogType(), objectName + " does not exist in bucket " + fixedBucketName);
        return false;
    }

    public String getObjectUrl(String bucketName, String objectName) {
        String fixedBucketName = fixBucketName(bucketName);
        return "https://" + fixedBucketName + ".s3." + region.toString() + ".amazonaws.com/" + objectName;
    }

	// Calls the private object verification method.
    public boolean verifyObject(String bucketName, String objectName) {
        return verifyBucketObject(bucketName, objectName);
    }

    // Downloads an object from the specified bucket, into the specified file path.
    private boolean downloadObject(S3TransferManager transferManager, String bucketName, String key, String downloadFilePath) {
        String fixedBucketName = fixBucketName(bucketName);
        if (!verifyBucketObject(bucketName, key))
            return false;

        DownloadFileRequest request = DownloadFileRequest.builder()
                    .getObjectRequest(b -> b.bucket(fixedBucketName).key(key))
                    .addTransferListener(LoggingTransferListener.create())
                    .destination(Paths.get(downloadFilePath))
                    .build();

        FileDownload fileDownload = transferManager.downloadFile(request);

        fileDownload.completionFuture().join();

        return true;
    }

    // Calls the private object download method.
    public boolean downloadObject(String bucketName, String fileName, String downloadFilePath) {
        S3TransferManager manager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();

        return downloadObject(manager, bucketName, fileName, downloadFilePath);
    }

    public void deleteBucket(String bucketName) {
        String fixedBucketName = fixBucketName(bucketName);

        List<S3Object> bucketObjects = listBucketObjects(fixedBucketName);

        List<ObjectIdentifier> keys = new ArrayList<>();
        bucketObjects.forEach(s3Object -> keys.add(ObjectIdentifier.builder()
                .key(s3Object.key())
                .build()));

        Delete delete = Delete.builder()
                .objects(keys)
                .build();

        try {
            DeleteObjectsRequest request = DeleteObjectsRequest.builder()
                    .bucket(fixedBucketName)
                    .delete(delete)
                    .build();

            s3Client.deleteObjects(request);
        } catch (S3Exception e) {
            Bot.log(getLogType(), "Could not delete all objects from bucket " + fixedBucketName);
            return;
        }

        Bot.log(getLogType(), "Deleted all objects from bucket " + fixedBucketName);

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder()
                .bucket(fixedBucketName)
                .build();

        s3Client.deleteBucket(deleteBucketRequest);

        Bot.log(getLogType(), "Deleted bucket " + fixedBucketName);
    }

    private List<S3Object> listBucketObjects(String bucketName) {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);

        return new ArrayList<>(response.contents());
    }

	// Deletes an object from a specified bucket if it's currently present.
    private void deleteBucketObject(String bucketName, String fileName) {
        String fixedBucketName = fixBucketName(bucketName);

    	if (!verifyObject(fixedBucketName, fileName))
    		return;
    	
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(fixedBucketName)
                .key(fileName)
                .build();

        s3Client.deleteObject(request);
        Bot.log(getLogType(), fileName + " was deleted from bucket " + fixedBucketName);
    }

	// Calls the private object deletion method.
    public void deleteObject(String bucketName, String fileName) {
        deleteBucketObject(bucketName, fileName);
    }

	// Uploads an object to a specified bucket
    private void uploadObject(S3TransferManager transferManager, String bucketName, String fileName, String filePath) {
        String fixedBucketName = fixBucketName(bucketName);

        UploadFileRequest request = UploadFileRequest.builder()
                .putObjectRequest(r -> r
                        .bucket(fixedBucketName)
                        .key(fileName)
                        .acl(ObjectCannedACL.PUBLIC_READ))
                .addTransferListener(LoggingTransferListener.create())
                .source(Paths.get(filePath))
                .build();

        FileUpload upload = transferManager.uploadFile(request);

        upload.completionFuture().join();
        Bot.log(getLogType(), "Uploaded " + fileName + " to bucket " + fixedBucketName);
    }

	// Calls the private object upload method.
    public void uploadObject(String bucketName, String fileName, String filePath) {
        S3TransferManager manager = S3TransferManager.builder()
                .s3Client(s3AsyncClient)
                .build();
        uploadObject(manager, bucketName, fileName, filePath);
    }



    // **************************************************************************************************************
    //
    //                                                 DynamoDB Stuff
    //
    // **************************************************************************************************************
    
    // Most operations were modified from examples in https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/java_dynamodb_code_examples.html

    // If the necessary tables do not exist yet, their information is initialized here, and prepared to be created.
    private void initDynamoDB() {
        List<String> tableNames = new ArrayList<>();
        Map<String, AttributeDefinition> tableAttributes = new HashMap<>();

        tableNames.add("SoundByteInfo");
        tableAttributes.put("SoundByteInfo", AttributeDefinition.builder()
                .attributeName("ID")
                .attributeType("S")
                .build());

        tableNames.add("SoundByteServerList");
        tableAttributes.put("SoundByteServerList", AttributeDefinition.builder()
                .attributeName("ServerID")
                .attributeType("S")
                .build());

        tableNames.add("SoundByteResponses");
        tableAttributes.put("SoundByteResponses", AttributeDefinition.builder()
                .attributeName("Prompt")
                .attributeType("S")
                .build());

        for (String tableName : tableNames) {
            boolean result = verifyDynamoTable(DescribeTableRequest.builder().tableName(tableName).build());

            if (!result) {
                Bot.log(getLogType(), tableName + " table does not exist, creating now...");
                createTable(tableName, tableAttributes.get(tableName));
                Bot.log(getLogType(), tableName + " table was created");
            }
        }
    }

    // Verifies whether the DynamoDB Tables exist or not.
    private boolean verifyDynamoTable(DescribeTableRequest request) {
        try {
            dynamoDbClient.describeTable(request);
            return true;
        } catch (ResourceNotFoundException e)
        {
            return false;
        }
    }

    // Calls the private table verification method.
    public boolean verifyDynamoTable(String tableName){
        return verifyDynamoTable(DescribeTableRequest.builder().tableName(tableName).build());
    }

    // Adds an item with its attributes to a specified table.
    public void addTableItem(String tableName, List<String> keys, List<String> keyVals) {
        Map<String, AttributeValue> items = new HashMap<>();
        for (int i = 0; i < keys.size(); i++)
        {
            items.put(keys.get(i), AttributeValue.builder().s(keyVals.get(i)).build());
        }

        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(items)
                .build();

        dynamoDbClient.putItem(request);
        Bot.log(getLogType(), "Successfully added items to " + tableName);
    }

    public void deleteTableItem(String tableName, String key, String keyVal) {

        if (!verifyDynamoTable(tableName))
        {
            Bot.log(getLogType(), "Could not find and delete item " + keyVal + " from table " + tableName);
            return;
        }

        Map<String, AttributeValue> item = new HashMap<>();
        item.put(key, AttributeValue.builder()
                .s(keyVal)
                .build());

        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(tableName)
                .key(item)
                .build();

        try {
            dynamoDbClient.deleteItem(request);
        } catch (DynamoDbException e) {
            Bot.log(getLogType(), "Could not delete item " + keyVal + " from table " + tableName);
            return;
        }

        Bot.log(getLogType(), "Deleted item " + keyVal + " from table " + tableName);
    }

    // Scans a table and returns the response. Responses are analyzed for the data outside this method to make things simpler.
    public ScanResponse scanItems(String tableName) {

        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .build();

        return dynamoDbClient.scan(request);
    }

    // Retrieves an item from a specified table.
    public Map<String, AttributeValue> getItem(String tableName, String key, String keyVal) {
        Map<String, AttributeValue> item = new HashMap<>();

        item.put(key, AttributeValue.builder()
                .s(keyVal)
                .build());

        GetItemRequest request = GetItemRequest.builder()
                .key(item)
                .tableName(tableName)
                .build();

        return dynamoDbClient.getItem(request).item();

    }

    // Updates an attribute for an item on a table.
    public void updateTableItem(String tableName, String key, String keyVal, String name, String updateVal) {

        HashMap<String,AttributeValue> itemKey = new HashMap<>();

        itemKey.put(key, AttributeValue.builder().s(keyVal).build());

        HashMap<String,AttributeValueUpdate> updatedValues = new HashMap<>();

        // Update the column specified by variable name with variable updatedVal.
        updatedValues.put(name, AttributeValueUpdate.builder()
                .value(AttributeValue.builder().s(updateVal).build())
                .action(AttributeAction.PUT)
                .build());

        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(tableName)
                .key(itemKey)
                .attributeUpdates(updatedValues)
                .build();

        try {
            dynamoDbClient.updateItem(request);
        } catch (DynamoDbException e) {
            Bot.log(getLogType(), "Could not update item in table: " + request.tableName());
        }
    }

    // Creates a DynamoDB table with the partition key already prepared.
    private void createTable(String tableName, AttributeDefinition tableAttribute) {
        DynamoDbWaiter dynamoDbWaiter = dynamoDbClient.waiter();
        KeySchemaElement tableKey;

        tableKey = KeySchemaElement.builder()
                .attributeName(tableAttribute.attributeName())
                .keyType(KeyType.HASH)
                .build();

        CreateTableRequest request = CreateTableRequest.builder()
                .keySchema(tableKey)
                // If throughput is high and steady, uncomment this and comment the billingMode call to disable ON_DEMAND payment
                /*.provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(10L)
                        .writeCapacityUnits(10L)
                        .build())*/
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .attributeDefinitions(tableAttribute)
                .tableName(tableName)
                .build();

        dynamoDbClient.createTable(request);
        DescribeTableRequest describeTableRequest = DescribeTableRequest.builder()
                .tableName(tableName)
                .build();

        dynamoDbWaiter.waitUntilTableExists(describeTableRequest);
    }

    // Calls the private table creation method.
    public void createDynamoTable(String tableName, AttributeDefinition tableAttributes) {
        createTable(tableName, tableAttributes);
    }

    public void deleteTable(String tableName) {

        if (!verifyDynamoTable(tableName))
        {
            Bot.log(getLogType(), "Could not find and delete table " + tableName);
            return;
        }

        DeleteTableRequest request = DeleteTableRequest.builder()
                .tableName(tableName)
                .build();

        try {
            dynamoDbClient.deleteTable(request);
        } catch (DynamoDbException e) {
            Bot.log(getLogType(), "Could not delete table " + tableName);
            return;
        }

        Bot.log(getLogType(), "Deleted table " + tableName);
    }
}
