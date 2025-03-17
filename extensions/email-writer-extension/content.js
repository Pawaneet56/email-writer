console.log("Email workspace entered")

function findComposeToolbar(){
const selectors=['.btC','.aDh','role="toolbar"','.gU.Up'];
for(const selector of selectors){
const toolbar=document.querySelector(selector);
if(toolbar)
{
return toolbar;}
}
return null;
}
function getEmailContent(){
    const selectors=['.h7','.a3s.aiL','role="presentation"','.gmail_quote'];
    for(const selector of selectors){
    const content=document.querySelector(selector);
    if(content)
    {
    return content.innerText.trim();}
    }
    return '';
    }
function createAIButton(){
    const button= document.createElement('div');
    button.className='T-I J-J5-Ji aoO v7 T-I-atl L3';
    button.style.marginRight = '8px';
    button.innerHTML='AI Reply';
    button.setAttribute('role','button');
    button.setAttribute('data-tooltip','Generate AI Reply');
    return button;
}

function injectButton(){
     const existingButton=document.querySelector('.ai-reply-button');
     if(existingButton) existingButton.remove();

     const toolBar= findComposeToolbar();
     if(!toolBar){
     console.log("Toolbar not found");
     return;
     }
     else{
     console.log("Toolbar found, creating AI button")
     }
     const button=createAIButton();
     button.classList.add('ai-reply-button');

     button.addEventListener('click',async()=>{
        try{
            button.innerHTML='Generating ....';
            button.disabled=true;
            const emailContent=getEmailContent();
            const response=await fetch('http://localhost:8080/api/email/generate-email',{
                method: 'POST',
                headers:{
                    'Content-Type':'application/json',
                },
                body: JSON.stringify({
                    emailContent: emailContent,
                    tone: "professional"

                })
            });
            if(!response.ok){
                throw new Error('API Request Failed');
            }

            const generatedReply=await response.text();
            const composedBox=document.querySelector('[g_editable="true"],[role="textbox"]')
            if(composedBox){
                composedBox.focus();
                document.execCommand('insertText','false',generatedReply);
            }
            else{
                console.error('ComposeBox not found');
            }

        }
        catch(error){
            console.error(error);
            alert('Failed to generate Reply');
        }
        finally{
            button.innerHTML='AI Reply';
            button.disabled=false;
        }
     });
     toolBar.insertBefore(button,toolBar.firstChild);
}
const observer = new MutationObserver((mutations) => {
    for (const mutation of mutations) {
        const addedNodes = Array.from(mutation.addedNodes);

        for (const node of addedNodes) {
            if (node.nodeType !== Node.ELEMENT_NODE) continue;

            if (node.matches('.aDh, .btC, [role="dialog"]') || node.querySelector('.aDh, .btC, [role="dialog"]')) {
                console.log("Compose Window detected");
                setTimeout(injectButton, 500);
            }
        }
    }
});

observer.observe(document.body, {
    childList: true,
    subtree: true
});
