import React, { Component, useState,useEffect} from "react";
import "../../node_modules/bootstrap/dist/css/bootstrap.css";
import "../../node_modules/bootstrap/dist/js/bootstrap.bundle";
import "../index.css";
import img1 from "../Image/slider01.jpg";
import img2 from "../Image/slider02.jpg";
import img3 from "../Image/slider03.jpg";

function Background (){

        return(
<div id="carouselExampleCaptions" class="carousel slide" data-bs-ride="carousel">
<div class="Overlay"></div>
  <div class="carousel-indicators">
    <button type="button" data-bs-target="#carouselExampleCaptions" data-bs-slide-to="0" class="active" aria-current="true" aria-label="Slide 1"></button>
    <button type="button" data-bs-target="#carouselExampleCaptions" data-bs-slide-to="1" aria-label="Slide 2"></button>
    <button type="button" data-bs-target="#carouselExampleCaptions" data-bs-slide-to="2" aria-label="Slide 3"></button>
  </div>
  <div class="carousel-inner">
  {/* <div class="Overlay"></div> */}
    <div class="carousel-item active">
    
    <img src={img1} style={{"width":"100%","height":"951px"}} alt="" />

      {/* <HTMLImageElement src="..." class="d-block w-100" alt="..."></HTMLImageElement> */}
      
    </div>
    <div class="carousel-item">
    <img src={img2} style={{"width":"100%","height":"951px"}} alt="" />
    </div>
    <div class="carousel-item">
    <img src={img3} style={{"width":"100%","height":"951px"}} alt="" />
    </div>
  </div>
</div>
        )
}
export default Background;