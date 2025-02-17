// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: ConsumerStore.proto

// Protobuf Java Version: 3.25.5
package com.ibm.test.g3store.grpc;

/**
 * Protobuf type {@code test.g3store.grpc.GenreRequest}
 */
public final class GenreRequest extends
    com.google.protobuf.GeneratedMessageV3 implements
    // @@protoc_insertion_point(message_implements:test.g3store.grpc.GenreRequest)
    GenreRequestOrBuilder {
private static final long serialVersionUID = 0L;
  // Use GenreRequest.newBuilder() to construct.
  private GenreRequest(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
    super(builder);
  }
  private GenreRequest() {
    genreType_ = 0;
  }

  @java.lang.Override
  @SuppressWarnings({"unused"})
  protected java.lang.Object newInstance(
      UnusedPrivateParameter unused) {
    return new GenreRequest();
  }

  public static final com.google.protobuf.Descriptors.Descriptor
      getDescriptor() {
    return com.ibm.test.g3store.grpc.AppConsumerProto.internal_static_test_g3store_grpc_GenreRequest_descriptor;
  }

  @java.lang.Override
  protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internalGetFieldAccessorTable() {
    return com.ibm.test.g3store.grpc.AppConsumerProto.internal_static_test_g3store_grpc_GenreRequest_fieldAccessorTable
        .ensureFieldAccessorsInitialized(
            com.ibm.test.g3store.grpc.GenreRequest.class, com.ibm.test.g3store.grpc.GenreRequest.Builder.class);
  }

  public static final int GENRETYPE_FIELD_NUMBER = 1;
  private int genreType_ = 0;
  /**
   * <code>.GenreType genreType = 1;</code>
   * @return The enum numeric value on the wire for genreType.
   */
  @java.lang.Override public int getGenreTypeValue() {
    return genreType_;
  }
  /**
   * <code>.GenreType genreType = 1;</code>
   * @return The genreType.
   */
  @java.lang.Override public com.ibm.test.g3store.grpc.GenreType getGenreType() {
    com.ibm.test.g3store.grpc.GenreType result = com.ibm.test.g3store.grpc.GenreType.forNumber(genreType_);
    return result == null ? com.ibm.test.g3store.grpc.GenreType.UNRECOGNIZED : result;
  }

  private byte memoizedIsInitialized = -1;
  @java.lang.Override
  public final boolean isInitialized() {
    byte isInitialized = memoizedIsInitialized;
    if (isInitialized == 1) return true;
    if (isInitialized == 0) return false;

    memoizedIsInitialized = 1;
    return true;
  }

  @java.lang.Override
  public void writeTo(com.google.protobuf.CodedOutputStream output)
                      throws java.io.IOException {
    if (genreType_ != com.ibm.test.g3store.grpc.GenreType.GAME.getNumber()) {
      output.writeEnum(1, genreType_);
    }
    getUnknownFields().writeTo(output);
  }

  @java.lang.Override
  public int getSerializedSize() {
    int size = memoizedSize;
    if (size != -1) return size;

    size = 0;
    if (genreType_ != com.ibm.test.g3store.grpc.GenreType.GAME.getNumber()) {
      size += com.google.protobuf.CodedOutputStream
        .computeEnumSize(1, genreType_);
    }
    size += getUnknownFields().getSerializedSize();
    memoizedSize = size;
    return size;
  }

  @java.lang.Override
  public boolean equals(final java.lang.Object obj) {
    if (obj == this) {
     return true;
    }
    if (!(obj instanceof com.ibm.test.g3store.grpc.GenreRequest)) {
      return super.equals(obj);
    }
    com.ibm.test.g3store.grpc.GenreRequest other = (com.ibm.test.g3store.grpc.GenreRequest) obj;

    if (genreType_ != other.genreType_) return false;
    if (!getUnknownFields().equals(other.getUnknownFields())) return false;
    return true;
  }

  @java.lang.Override
  public int hashCode() {
    if (memoizedHashCode != 0) {
      return memoizedHashCode;
    }
    int hash = 41;
    hash = (19 * hash) + getDescriptor().hashCode();
    hash = (37 * hash) + GENRETYPE_FIELD_NUMBER;
    hash = (53 * hash) + genreType_;
    hash = (29 * hash) + getUnknownFields().hashCode();
    memoizedHashCode = hash;
    return hash;
  }

  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return PARSER.parseFrom(data, extensionRegistry);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  public static com.ibm.test.g3store.grpc.GenreRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input);
  }

  public static com.ibm.test.g3store.grpc.GenreRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input);
  }
  public static com.ibm.test.g3store.grpc.GenreRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageV3
        .parseWithIOException(PARSER, input, extensionRegistry);
  }

  @java.lang.Override
  public Builder newBuilderForType() { return newBuilder(); }
  public static Builder newBuilder() {
    return DEFAULT_INSTANCE.toBuilder();
  }
  public static Builder newBuilder(com.ibm.test.g3store.grpc.GenreRequest prototype) {
    return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
  }
  @java.lang.Override
  public Builder toBuilder() {
    return this == DEFAULT_INSTANCE
        ? new Builder() : new Builder().mergeFrom(this);
  }

  @java.lang.Override
  protected Builder newBuilderForType(
      com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
    Builder builder = new Builder(parent);
    return builder;
  }
  /**
   * Protobuf type {@code test.g3store.grpc.GenreRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
      // @@protoc_insertion_point(builder_implements:test.g3store.grpc.GenreRequest)
      com.ibm.test.g3store.grpc.GenreRequestOrBuilder {
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return com.ibm.test.g3store.grpc.AppConsumerProto.internal_static_test_g3store_grpc_GenreRequest_descriptor;
    }

    @java.lang.Override
    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return com.ibm.test.g3store.grpc.AppConsumerProto.internal_static_test_g3store_grpc_GenreRequest_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              com.ibm.test.g3store.grpc.GenreRequest.class, com.ibm.test.g3store.grpc.GenreRequest.Builder.class);
    }

    // Construct using com.ibm.test.g3store.grpc.GenreRequest.newBuilder()
    private Builder() {

    }

    private Builder(
        com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
      super(parent);

    }
    @java.lang.Override
    public Builder clear() {
      super.clear();
      bitField0_ = 0;
      genreType_ = 0;
      return this;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
      return com.ibm.test.g3store.grpc.AppConsumerProto.internal_static_test_g3store_grpc_GenreRequest_descriptor;
    }

    @java.lang.Override
    public com.ibm.test.g3store.grpc.GenreRequest getDefaultInstanceForType() {
      return com.ibm.test.g3store.grpc.GenreRequest.getDefaultInstance();
    }

    @java.lang.Override
    public com.ibm.test.g3store.grpc.GenreRequest build() {
      com.ibm.test.g3store.grpc.GenreRequest result = buildPartial();
      if (!result.isInitialized()) {
        throw newUninitializedMessageException(result);
      }
      return result;
    }

    @java.lang.Override
    public com.ibm.test.g3store.grpc.GenreRequest buildPartial() {
      com.ibm.test.g3store.grpc.GenreRequest result = new com.ibm.test.g3store.grpc.GenreRequest(this);
      if (bitField0_ != 0) { buildPartial0(result); }
      onBuilt();
      return result;
    }

    private void buildPartial0(com.ibm.test.g3store.grpc.GenreRequest result) {
      int from_bitField0_ = bitField0_;
      if (((from_bitField0_ & 0x00000001) != 0)) {
        result.genreType_ = genreType_;
      }
    }

    @java.lang.Override
    public Builder clone() {
      return super.clone();
    }
    @java.lang.Override
    public Builder setField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.setField(field, value);
    }
    @java.lang.Override
    public Builder clearField(
        com.google.protobuf.Descriptors.FieldDescriptor field) {
      return super.clearField(field);
    }
    @java.lang.Override
    public Builder clearOneof(
        com.google.protobuf.Descriptors.OneofDescriptor oneof) {
      return super.clearOneof(oneof);
    }
    @java.lang.Override
    public Builder setRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        int index, java.lang.Object value) {
      return super.setRepeatedField(field, index, value);
    }
    @java.lang.Override
    public Builder addRepeatedField(
        com.google.protobuf.Descriptors.FieldDescriptor field,
        java.lang.Object value) {
      return super.addRepeatedField(field, value);
    }
    @java.lang.Override
    public Builder mergeFrom(com.google.protobuf.Message other) {
      if (other instanceof com.ibm.test.g3store.grpc.GenreRequest) {
        return mergeFrom((com.ibm.test.g3store.grpc.GenreRequest)other);
      } else {
        super.mergeFrom(other);
        return this;
      }
    }

    public Builder mergeFrom(com.ibm.test.g3store.grpc.GenreRequest other) {
      if (other == com.ibm.test.g3store.grpc.GenreRequest.getDefaultInstance()) return this;
      if (other.genreType_ != 0) {
        setGenreTypeValue(other.getGenreTypeValue());
      }
      this.mergeUnknownFields(other.getUnknownFields());
      onChanged();
      return this;
    }

    @java.lang.Override
    public final boolean isInitialized() {
      return true;
    }

    @java.lang.Override
    public Builder mergeFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      if (extensionRegistry == null) {
        throw new java.lang.NullPointerException();
      }
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            case 8: {
              genreType_ = input.readEnum();
              bitField0_ |= 0x00000001;
              break;
            } // case 8
            default: {
              if (!super.parseUnknownField(input, extensionRegistry, tag)) {
                done = true; // was an endgroup tag
              }
              break;
            } // default:
          } // switch (tag)
        } // while (!done)
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.unwrapIOException();
      } finally {
        onChanged();
      } // finally
      return this;
    }
    private int bitField0_;

    private int genreType_ = 0;
    /**
     * <code>.GenreType genreType = 1;</code>
     * @return The enum numeric value on the wire for genreType.
     */
    @java.lang.Override public int getGenreTypeValue() {
      return genreType_;
    }
    /**
     * <code>.GenreType genreType = 1;</code>
     * @param value The enum numeric value on the wire for genreType to set.
     * @return This builder for chaining.
     */
    public Builder setGenreTypeValue(int value) {
      genreType_ = value;
      bitField0_ |= 0x00000001;
      onChanged();
      return this;
    }
    /**
     * <code>.GenreType genreType = 1;</code>
     * @return The genreType.
     */
    @java.lang.Override
    public com.ibm.test.g3store.grpc.GenreType getGenreType() {
      com.ibm.test.g3store.grpc.GenreType result = com.ibm.test.g3store.grpc.GenreType.forNumber(genreType_);
      return result == null ? com.ibm.test.g3store.grpc.GenreType.UNRECOGNIZED : result;
    }
    /**
     * <code>.GenreType genreType = 1;</code>
     * @param value The genreType to set.
     * @return This builder for chaining.
     */
    public Builder setGenreType(com.ibm.test.g3store.grpc.GenreType value) {
      if (value == null) {
        throw new NullPointerException();
      }
      bitField0_ |= 0x00000001;
      genreType_ = value.getNumber();
      onChanged();
      return this;
    }
    /**
     * <code>.GenreType genreType = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearGenreType() {
      bitField0_ = (bitField0_ & ~0x00000001);
      genreType_ = 0;
      onChanged();
      return this;
    }
    @java.lang.Override
    public final Builder setUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.setUnknownFields(unknownFields);
    }

    @java.lang.Override
    public final Builder mergeUnknownFields(
        final com.google.protobuf.UnknownFieldSet unknownFields) {
      return super.mergeUnknownFields(unknownFields);
    }


    // @@protoc_insertion_point(builder_scope:test.g3store.grpc.GenreRequest)
  }

  // @@protoc_insertion_point(class_scope:test.g3store.grpc.GenreRequest)
  private static final com.ibm.test.g3store.grpc.GenreRequest DEFAULT_INSTANCE;
  static {
    DEFAULT_INSTANCE = new com.ibm.test.g3store.grpc.GenreRequest();
  }

  public static com.ibm.test.g3store.grpc.GenreRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static final com.google.protobuf.Parser<GenreRequest>
      PARSER = new com.google.protobuf.AbstractParser<GenreRequest>() {
    @java.lang.Override
    public GenreRequest parsePartialFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      Builder builder = newBuilder();
      try {
        builder.mergeFrom(input, extensionRegistry);
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(builder.buildPartial());
      } catch (com.google.protobuf.UninitializedMessageException e) {
        throw e.asInvalidProtocolBufferException().setUnfinishedMessage(builder.buildPartial());
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(e)
            .setUnfinishedMessage(builder.buildPartial());
      }
      return builder.buildPartial();
    }
  };

  public static com.google.protobuf.Parser<GenreRequest> parser() {
    return PARSER;
  }

  @java.lang.Override
  public com.google.protobuf.Parser<GenreRequest> getParserForType() {
    return PARSER;
  }

  @java.lang.Override
  public com.ibm.test.g3store.grpc.GenreRequest getDefaultInstanceForType() {
    return DEFAULT_INSTANCE;
  }

}

