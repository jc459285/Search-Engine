import React, { Component, useState,useEffect } from "react";
import "../../node_modules/bootstrap/dist/css/bootstrap.css";
import "../../node_modules/@fortawesome/fontawesome-free/css/all.css";
import {useNavigate} from "react-router-dom";



function SearchBlock (){
    const nav=useNavigate();
    const [historyData,setHistoryData]=useState([]);
    const [value,setValue]=useState("");  
    function fileration(data){
        return(data.search.toString().toLocaleLowerCase().startsWith(value));
    }
    function submit(e){
        if(e.code==="Enter"||e.code==="NumpadEnter")
        {
            e.preventDefault();
            console.log(
                "the enter key is pressed"
            );
            var found=false;
            if(!(e.target.value.trim()===""))
            {
                fetch(" http://localhost:8000/History").then(response=>{
                    if(response.ok)
                    {
                        return response.json();
                    }
                    }).then(data=>{
                    for(let i =0;i<data.length;i++)
                    {
                        console.log(data[i].search.toLowerCase()+" === "+e.target.value.trim().toLowerCase());
                        if(e.target.value.trim().toLowerCase()===data[i].search.toLowerCase())
                        {
                            found=true;
                            break;
                        }
                        
                    }
                    
                })
                setTimeout(()=>{if(!found)
                {
                    const doc=
                    {
                        search:e.target.value.trim()
                    }
                console.log(doc);
                fetch("http://localhost:8000/History",{
                    method:"POST",
                    body:JSON.stringify(doc),
                    headers:{"Content-Type":"application/json"}});
                    console.log("submitedd");
                    console.log(doc);
                }
                console.log("nava");
                nav("Results/"+value);
                console.log("nava2");
            },500)
                
            
            }

        }
        
    }
    function add(suggest){
        setTimeout(()=>{
        setValue(suggest);
        },);
        
    }   
    function inc(e){
        
        if(e.code!=="Enter"||e.code!=="NumpadEnter")
        {
            console.log("the function is ready");
        setValue(e.target.value);
        console.log(value);
        }
        
        if(e.target.value.trim()!=="")
        {
        
            fetch(" http://localhost:8000/History").then(response=>{
                if(response.ok)
                {
                    return response.json();
                }
                }).then(data=>{
                console.log(data);
                console.log(e.target.value);
                setHistoryData(data.filter
                    ((data)=>(data.search.toLocaleLowerCase().startsWith
                    (e.target.value.trim().toLowerCase()))));
            })
        }
        else{
            setHistoryData([]);
        }
    }
        return(
            <div className="container d-flex justify-content-center align-items-center " style={{"background-color": "transparent"}}>
                <div className="card mb-3 center-block" style={{"margin-top": "20%","width": "100%","border":"none","background-color": "transparent"}}>
                    <div className="row g-0">
                        <div className=" col-md-12">
                            <h1 className="text-center"style={{"color":"#198754","font-size":"5rem"}} >K3M</h1>
                            <div className="card-body">
                                <form >
                                <input className="form-control me-2"  type="search"  
                                onKeyDown={submit}
                                onChange={inc}
                                value={value} 
                                placeholder="Search" aria-label="Search" 
                                style={{"padding": "20px","border-radius": "30px"
                                ,"font-size": "21px","background-color": "#fff"
                                ,"border": "none","color":"#212529"}} 
                                
                                />
                                {/* <p>{Recongnition.}</p> */}
                                </form>
                                <div className="Slider mt-2 " style={{"border-radius":"10px" , "scrollbar-width": "none"}}>
                                    {historyData.map((record)=>{return (<p className="p-2" style={{"cursor":"pointer"}}  onClick={()=>add(record.search)}>{record.search}</p>)})}
                                </div>
                            </div>
                        </div>
                </div>
            </div>
            </div>
        )
}
export default SearchBlock;