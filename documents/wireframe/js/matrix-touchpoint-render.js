/* =========================
   DATA MODEL
========================= */

const model = {
  technologies: [
    { key: "self", label: "Self-service" },
    { key: "chatbot", label: "Chatbots" },
    { key: "voice", label: "Voice assistants" },
    { key: "ar", label: "Augmented reality" },
    { key: "vr", label: "Virtual reality" },
    { key: "robot", label: "Service robots" }
  ],
  stages: [
    {
      name: "Pre-purchase",
      color: "#f9a825",
      rows: [
        { text: "Evaluating alternatives", data: { chatbot:true, voice:true, ar:true, vr:true } },
        { text: "Providing personalised recommendations", data: { chatbot:true, voice:true, robot:true } },
        { text: "Answering queries", data: { chatbot:true, voice:true, robot:true } },
        { text: "Advertising", data: { ar:true, vr:true } },
        { text: "Social media interactions", data: { chatbot:true, ar:true } }
      ]
    },
    {
      name: "Purchase",
      color: "#1976d2",
      rows: [
        { text: "Payment", data: { self:true, chatbot:true, voice:true, ar:true, robot:true } },
        { text: "Ordering", data: { chatbot:true, voice:true, robot:true } },
        { text: "Product choice", data: { chatbot:true, voice:true, ar:true, vr:true } },
        { text: "Product presentation", data: { ar:true, vr:true } }
      ]
    },
    {
      name: "Post-purchase",
      color: "#2e7d32",
      rows: [
        { text: "Customer support", data: { chatbot:true, voice:true, robot:true } },
        { text: "Customer complaint", data: { chatbot:true, voice:true, robot:true } },
        { text: "Community building", data: { ar:true } },
        { text: "Service recovery", data: { robot:true } },
        { text: "Product delivery", data: { robot:true } }
      ]
    }
  ]
};

/* =========================
   LAYOUT CONSTANTS
========================= */

const HEADER_Y = 100;
const HEADER_H = 50;
const ROW_H = 44;

const X_STAGE = 60;
const STAGE_W = 120;

const X_TOUCH = 180;
const TOUCH_W = 300;

const COL_W = 180;
const TECH_X = 480;

/* =========================
   SVG SETUP
========================= */

const svg = document.getElementById("chart");

function svgEl(tag, attrs={}, text) {
  const el = document.createElementNS("http://www.w3.org/2000/svg", tag);
  Object.entries(attrs).forEach(([k,v]) => el.setAttribute(k,v));
  if (text) el.textContent = text;
  return el;
}

/* =========================
   CALCULATE TOTAL HEIGHT
========================= */

const totalRows = model.stages.reduce((sum,s)=>sum+s.rows.length,0);
const contentHeight = HEADER_Y + HEADER_H + totalRows * ROW_H + 40;

/* IMPORTANT FIX */
svg.setAttribute("viewBox", `0 0 1560 ${contentHeight}`);
svg.setAttribute("height", contentHeight);

/* =========================
   TITLE
========================= */

svg.append(
  svgEl("text",{x:780,y:40,"text-anchor":"middle",class:"title"},
    "Key Touchpoints Impacted by Artificial Intelligence"),
  svgEl("text",{x:780,y:68,"text-anchor":"middle",class:"subtitle"},
    "Along the Customer Journey")
);

/* =========================
   HEADER
========================= */

svg.append(svgEl("rect",{x:X_STAGE,y:HEADER_Y,width:STAGE_W,height:HEADER_H,fill:"#34495e"}));
svg.append(svgEl("rect",{x:X_TOUCH,y:HEADER_Y,width:TOUCH_W,height:HEADER_H,fill:"#2c3e50"}));
svg.append(svgEl("text",{x:X_TOUCH+TOUCH_W/2,y:HEADER_Y+32,class:"header","text-anchor":"middle"},"Touchpoints"));

model.technologies.forEach((t,i)=>{
  const x = TECH_X + i*COL_W;
  svg.append(svgEl("rect",{x,y:HEADER_Y,width:COL_W,height:HEADER_H,fill:"#303f9f"}));
  svg.append(svgEl("text",{x:x+COL_W/2,y:HEADER_Y+32,class:"header","text-anchor":"middle"},t.label));
});

/* =========================
   GRID LINES (VERTICAL)
========================= */

[
  X_TOUCH,
  X_TOUCH+TOUCH_W,
  TECH_X,
  TECH_X+COL_W,
  TECH_X+COL_W*2,
  TECH_X+COL_W*3,
  TECH_X+COL_W*4,
  TECH_X+COL_W*5,
  TECH_X+COL_W*6
].forEach(x=>{
  svg.append(svgEl("line",{x1:x,y1:HEADER_Y,x2:x,y2:contentHeight,class:"col-line"}));
});

/* =========================
   ROWS
========================= */

let y = HEADER_Y + HEADER_H;
let idx = 0;

model.stages.forEach(stage=>{
  const stageHeight = stage.rows.length * ROW_H;

  svg.append(svgEl("rect",{x:X_STAGE,y,width:STAGE_W,height:stageHeight,fill:stage.color}));
  svg.append(svgEl("text",{x:X_STAGE+STAGE_W/2,y:y+stageHeight/2,class:"stage-text","text-anchor":"middle"},stage.name));

  stage.rows.forEach(row=>{
    svg.append(svgEl("rect",{
      x:X_TOUCH,
      y,
      width:TECH_X+COL_W*model.technologies.length-X_TOUCH,
      height:ROW_H,
      class: idx%2 ? "row-odd row-bg" : "row-even row-bg"
    }));

    svg.append(svgEl("text",{x:X_TOUCH+10,y:y+28,class:"cell-text","data-touchpoint":row.text},row.text));

    model.technologies.forEach((t,i)=>{
      if(row.data[t.key]){
        svg.append(svgEl("text",{
          x:TECH_X+i*COL_W+COL_W/2,
          y:y+28,
          class:"check",
          "text-anchor":"middle",
          "data-tech":t.key,
          "data-touchpoint":row.text
        },"✓"));
      }
    });

    svg.append(svgEl("line",{x1:X_TOUCH,y1:y,x2:TECH_X+COL_W*model.technologies.length,y2:y,class:"grid-line"}));

    y+=ROW_H;
    idx++;
  });
});

/* =========================
   INTERACTION
========================= */

$(document).on("click",".cell-text",e=>{
  alert("Touchpoint: "+$(e.target).data("touchpoint"));
});

$(document).on("click",".check",e=>{
  alert(
    "Technology: "+$(e.target).data("tech")+
    "\nTouchpoint: "+$(e.target).data("touchpoint")
  );
});
