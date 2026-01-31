package com.edurican.enchelinbe.common;

import java.util.List;

public record Page<T>(List<T> contents, Boolean hasNext) {

}